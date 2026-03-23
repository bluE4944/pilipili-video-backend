package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.entity.out.VideoPlayHistoryItem;
import com.pilipili.entity.out.VideoPlaySourceInfo;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.repository.VideoPlayHistoryRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 视频播放服务类
 *
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoPlayService {

    private static final String TRANSCODE_DIR = "transcodes";
    private static final String COMPATIBLE_MP4_NAME = "compatible.mp4";
    private static final String HLS_PLAYLIST_NAME = "index.m3u8";

    private final VideoRepository videoRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoPlayHistoryRepository videoPlayHistoryRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${video.transcode.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${video.transcode.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final ConcurrentHashMap<Long, Object> compatibleLocks = new ConcurrentHashMap<>();

    /**
     * 获取视频播放地址（带防盗链和时效控制）
     */
    public String getVideoPlayUrl(Long videoId, Long expireSeconds) {
        return getVideoPlaySourceInfo(videoId, expireSeconds).getPlayUrl();
    }

    public VideoPlaySourceInfo getVideoPlaySourceInfo(Long videoId, Long expireSeconds) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        VideoPlaySourceInfo sourceInfo = new VideoPlaySourceInfo();

        long expireAt = expireSeconds != null ? (System.currentTimeMillis() / 1000 + expireSeconds) : 0L;
        if (expireAt > 0) {
            sourceInfo.setPlayUrl("/api/video/stream/" + videoId + "?expire=" + expireAt);
        } else {
            sourceInfo.setPlayUrl("/api/video/stream/" + videoId);
        }
        sourceInfo.setSourceMode("direct");
        sourceInfo.setProcessMode("none");
        sourceInfo.setBrowserFallbackAllowed(shouldUseCompatibleProcessing(video));
        return sourceInfo;
    }

    /**
     * 解析视频流播放信息
     */
    public VideoStreamInfo getVideoStreamInfo(Long videoId) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        VideoEpisode episode = videoEpisodeRepository.getOne(new QueryWrapper<VideoEpisode>().eq("video_id", videoId));
        if (episode != null && episode.getFilePath() != null && !episode.getFilePath().isEmpty()) {
            return new VideoStreamInfo(Paths.get(episode.getFilePath()), null);
        }

        String videoUrl = video.getVideoUrl();
        if (videoUrl == null || videoUrl.isEmpty()) {
            return new VideoStreamInfo(null, null);
        }

        String normalized = stripQuery(videoUrl);
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return new VideoStreamInfo(null, normalized);
        }

        Path localPath = resolveLocalPath(normalized);
        return new VideoStreamInfo(localPath, null);
    }

    public Path prepareHlsStream(Long videoId) {
        CompatibleOutput output = prepareCompatibleOutput(videoId);
        if (output.getOutputMode() != OutputMode.HLS || output.getPlaylistPath() == null) {
            throw new RuntimeException("当前视频无需 HLS 输出");
        }
        return output.getPlaylistPath();
    }

    public Path getCompatibleMp4Path(Long videoId) {
        CompatibleOutput output = prepareCompatibleOutput(videoId);
        if (output.getOutputMode() != OutputMode.MP4 || output.getMp4Path() == null) {
            throw new RuntimeException("当前视频未生成兼容 MP4");
        }
        return output.getMp4Path();
    }

    public Path getHlsOutputDir(Long videoId) {
        return getOutputDir(videoId);
    }

    private CompatibleOutput prepareCompatibleOutput(Long videoId) {
        CompatiblePlan plan = resolveCompatiblePlan(videoId);
        Path outputDir = getOutputDir(videoId);
        Path mp4Path = outputDir.resolve(COMPATIBLE_MP4_NAME);
        Path playlistPath = outputDir.resolve(HLS_PLAYLIST_NAME);

        if (plan.getOutputMode() == OutputMode.MP4 && Files.exists(mp4Path)) {
            return new CompatibleOutput(OutputMode.MP4, mp4Path, null);
        }
        if (plan.getOutputMode() == OutputMode.HLS && Files.exists(playlistPath)) {
            return new CompatibleOutput(OutputMode.HLS, null, playlistPath);
        }

        Object lock = compatibleLocks.computeIfAbsent(videoId, key -> new Object());
        synchronized (lock) {
            try {
                Files.createDirectories(outputDir);
                if (plan.getOutputMode() == OutputMode.MP4) {
                    if (!Files.exists(mp4Path)) {
                        prepareMp4Output(videoId, plan, mp4Path);
                    }
                    return new CompatibleOutput(OutputMode.MP4, mp4Path, null);
                }

                if (!Files.exists(playlistPath)) {
                    prepareHlsOutput(videoId, plan, outputDir);
                }
                return new CompatibleOutput(OutputMode.HLS, null, playlistPath);
            } catch (IOException e) {
                throw new RuntimeException("创建兼容播放目录失败", e);
            } finally {
                compatibleLocks.remove(videoId, lock);
            }
        }
    }

    private CompatiblePlan resolveCompatiblePlan(Long videoId) {
        VideoStreamInfo streamInfo = getVideoStreamInfo(videoId);
        if (streamInfo.getRemoteUrl() != null) {
            throw new RuntimeException("远程视频无需兼容处理");
        }
        Path sourcePath = streamInfo.getFilePath();
        if (sourcePath == null || !Files.exists(sourcePath) || Files.isDirectory(sourcePath)) {
            throw new RuntimeException("视频文件不存在");
        }

        MediaProfile profile = probeMediaProfile(sourcePath);
        String videoCodec = profile.getVideoCodec();
        String audioCodec = profile.getAudioCodec();

        if (isMp4VideoCodecCompatible(videoCodec)) {
            if (audioCodec.isEmpty() || isMp4AudioCodecCompatible(audioCodec)) {
                return new CompatiblePlan(sourcePath, profile, OutputMode.MP4, ProcessMode.REMUX);
            }
            return new CompatiblePlan(sourcePath, profile, OutputMode.MP4, ProcessMode.AUDIO_TRANSCODE);
        }

        return new CompatiblePlan(sourcePath, profile, OutputMode.HLS, ProcessMode.FULL_TRANSCODE);
    }

    private boolean shouldUseCompatibleProcessing(Video video) {
        if (video == null || video.getId() == null) {
            return false;
        }

        String format = normalizeExtension(video.getFormat());
        if (!format.isEmpty()) {
            return "mkv".equals(format);
        }

        VideoStreamInfo streamInfo = getVideoStreamInfo(video.getId());
        if (streamInfo.getRemoteUrl() != null) {
            return false;
        }
        Path filePath = streamInfo.getFilePath();
        if (filePath == null || filePath.getFileName() == null) {
            return false;
        }
        return "mkv".equals(normalizeExtension(filePath.getFileName().toString()));
    }

    private void prepareMp4Output(Long videoId, CompatiblePlan plan, Path outputPath) {
        List<String> command = new ArrayList<>();
        command.add(resolveFfmpegExecutable());
        command.add("-y");
        command.add("-i");
        command.add(plan.getSourcePath().toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a?");
        command.add("-sn");
        command.add("-movflags");
        command.add("+faststart");
        command.add("-c:v");
        command.add("copy");
        if (plan.getProcessMode() == ProcessMode.REMUX) {
            command.add("-c:a");
            command.add("copy");
        } else {
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("192k");
            command.add("-ac");
            command.add("2");
        }
        command.add(outputPath.toString());

        executeCommand(command, "生成兼容 MP4", videoId);
        if (!Files.exists(outputPath)) {
            throw new RuntimeException("兼容 MP4 生成失败");
        }
        log.info("生成兼容 MP4 完成: videoId={}, mode={}, videoCodec={}, audioCodec={}",
                videoId,
                plan.getProcessMode(),
                plan.getMediaProfile().getVideoCodec(),
                plan.getMediaProfile().getAudioCodec());
    }

    private void prepareHlsOutput(Long videoId, CompatiblePlan plan, Path outputDir) {
        Path playlistPath = outputDir.resolve(HLS_PLAYLIST_NAME);
        Path segmentPattern = outputDir.resolve("segment_%03d.ts");

        List<String> command = new ArrayList<>();
        command.add(resolveFfmpegExecutable());
        command.add("-y");
        command.add("-i");
        command.add(plan.getSourcePath().toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a?");
        command.add("-sn");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("veryfast");
        command.add("-crf");
        command.add("23");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-ac");
        command.add("2");
        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("6");
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_segment_filename");
        command.add(segmentPattern.toString());
        command.add(playlistPath.toString());

        executeCommand(command, "生成 HLS 输出", videoId);
        if (!Files.exists(playlistPath)) {
            throw new RuntimeException("HLS 输出生成失败");
        }
        log.info("生成 HLS 输出完成: videoId={}, videoCodec={}, audioCodec={}",
                videoId,
                plan.getMediaProfile().getVideoCodec(),
                plan.getMediaProfile().getAudioCodec());
    }

    private MediaProfile probeMediaProfile(Path sourcePath) {
        List<String> command = new ArrayList<>();
        command.add(resolveFfprobeExecutable());
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("stream=codec_name,codec_type");
        command.add("-of");
        command.add("compact=p=0:nk=1");
        command.add(sourcePath.toString());

        String output = executeCommand(command, "探测媒体编码", null);
        if (output == null || output.trim().isEmpty()) {
            return new MediaProfile("", "");
        }

        String videoCodec = "";
        String audioCodec = "";
        String[] lines = output.trim().split("\\R");
        for (String line : lines) {
            String[] parts = line.trim().split("\\|");
            if (parts.length < 2) {
                continue;
            }
            String codec = parts[0].trim().toLowerCase(Locale.ROOT);
            String type = parts[1].trim().toLowerCase(Locale.ROOT);
            if ("video".equals(type) && videoCodec.isEmpty()) {
                videoCodec = codec;
            } else if ("audio".equals(type) && audioCodec.isEmpty()) {
                audioCodec = codec;
            }
            if (!videoCodec.isEmpty() && !audioCodec.isEmpty()) {
                break;
            }
        }
        return new MediaProfile(videoCodec, audioCodec);
    }

    private String executeCommand(List<String> command, String action, Long videoId) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output;
            try (InputStream inputStream = process.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                output = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("{}失败: videoId={}, exitCode={}, command={}, output={}",
                        action, videoId, exitCode, String.join(" ", command), output);
                throw new RuntimeException(action + "失败");
            }
            return output == null ? "" : output;
        } catch (IOException e) {
            throw new RuntimeException(action + "失败，无法启动外部命令", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(action + "被中断", e);
        }
    }

    private boolean isMp4VideoCodecCompatible(String codec) {
        return "h264".equals(codec) || "avc1".equals(codec);
    }

    private boolean isMp4AudioCodecCompatible(String codec) {
        return "aac".equals(codec)
                || "mp3".equals(codec)
                || "mp2".equals(codec)
                || "alac".equals(codec);
    }

    private String resolveFfmpegExecutable() {
        return ffmpegPath == null || ffmpegPath.trim().isEmpty() ? "ffmpeg" : ffmpegPath.trim();
    }

    private String resolveFfprobeExecutable() {
        return ffprobePath == null || ffprobePath.trim().isEmpty() ? "ffprobe" : ffprobePath.trim();
    }

    private Path getOutputDir(Long videoId) {
        return Paths.get(uploadPath).resolve(TRANSCODE_DIR).resolve(String.valueOf(videoId));
    }

    private String stripQuery(String url) {
        int idx = url.indexOf("?");
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    private Path resolveLocalPath(String url) {
        String normalized = url;
        if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) {
            normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
            normalized = normalized.substring("uploads/".length());
            return Paths.get(uploadPath).resolve(normalized);
        }

        Path path = Paths.get(normalized);
        if (path.isAbsolute()) {
            return path;
        }

        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return Paths.get(uploadPath).resolve(normalized);
    }

    private String normalizeExtension(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int lastHashIndex = normalized.indexOf('#');
        if (lastHashIndex >= 0) {
            normalized = normalized.substring(0, lastHashIndex);
        }
        int lastDotIndex = normalized.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < normalized.length() - 1) {
            normalized = normalized.substring(lastDotIndex + 1);
        }
        return normalized;
    }

    public static class VideoStreamInfo {
        private final Path filePath;
        private final String remoteUrl;

        public VideoStreamInfo(Path filePath, String remoteUrl) {
            this.filePath = filePath;
            this.remoteUrl = remoteUrl;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getRemoteUrl() {
            return remoteUrl;
        }
    }

    private enum OutputMode {
        MP4,
        HLS
    }

    private enum ProcessMode {
        REMUX,
        AUDIO_TRANSCODE,
        FULL_TRANSCODE
    }

    private static class MediaProfile {
        private final String videoCodec;
        private final String audioCodec;

        private MediaProfile(String videoCodec, String audioCodec) {
            this.videoCodec = videoCodec == null ? "" : videoCodec.trim().toLowerCase(Locale.ROOT);
            this.audioCodec = audioCodec == null ? "" : audioCodec.trim().toLowerCase(Locale.ROOT);
        }

        public String getVideoCodec() {
            return videoCodec;
        }

        public String getAudioCodec() {
            return audioCodec;
        }
    }

    private static class CompatiblePlan {
        private final Path sourcePath;
        private final MediaProfile mediaProfile;
        private final OutputMode outputMode;
        private final ProcessMode processMode;

        private CompatiblePlan(Path sourcePath, MediaProfile mediaProfile, OutputMode outputMode, ProcessMode processMode) {
            this.sourcePath = sourcePath;
            this.mediaProfile = mediaProfile;
            this.outputMode = outputMode;
            this.processMode = processMode;
        }

        public Path getSourcePath() {
            return sourcePath;
        }

        public MediaProfile getMediaProfile() {
            return mediaProfile;
        }

        public OutputMode getOutputMode() {
            return outputMode;
        }

        public ProcessMode getProcessMode() {
            return processMode;
        }
    }

    private static class CompatibleOutput {
        private final OutputMode outputMode;
        private final Path mp4Path;
        private final Path playlistPath;

        private CompatibleOutput(OutputMode outputMode, Path mp4Path, Path playlistPath) {
            this.outputMode = outputMode;
            this.mp4Path = mp4Path;
            this.playlistPath = playlistPath;
        }

        public OutputMode getOutputMode() {
            return outputMode;
        }

        public Path getMp4Path() {
            return mp4Path;
        }

        public Path getPlaylistPath() {
            return playlistPath;
        }
    }

    /**
     * 记录播放进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordPlayProgress(Long videoId, Long userId, Integer progress, Integer playDuration, Integer quality, Double playbackRate) {
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        VideoPlayHistory history = videoPlayHistoryRepository.getOne(wrapper);

        if (history == null) {
            history = new VideoPlayHistory();
            history.setVideoId(videoId);
            history.setUserId(userId);
            history.setCreateTime(new Date());
            history.setLogicDel(0);
        }

        history.setProgress(progress);
        history.setPlayDuration(playDuration);
        history.setQuality(quality);
        history.setPlaybackRate(playbackRate);
        history.setUpdateTime(new Date());

        if (history.getId() == null) {
            videoPlayHistoryRepository.save(history);
        } else {
            videoPlayHistoryRepository.updateById(history);
        }

        log.debug("播放进度记录成功: videoId={}, userId={}, progress={}", videoId, userId, progress);
    }

    /**
     * 获取播放进度
     */
    public VideoPlayHistory getPlayProgress(Long videoId, Long userId) {
        if (videoId == null || userId == null) {
            return null;
        }

        Video video = videoRepository.getById(videoId);
        if (video != null) {
            return getPlayProgressByVideoId(videoId, userId);
        }

        VideoCollection collection = videoCollectionRepository.getById(videoId);
        if (collection == null) {
            return null;
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(
                new QueryWrapper<VideoEpisode>().eq("collection_id", videoId)
        );
        if (episodes == null || episodes.isEmpty()) {
            return null;
        }

        List<Long> videoIds = episodes.stream()
                .map(VideoEpisode::getVideoId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (videoIds.isEmpty()) {
            return null;
        }

        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.in("video_id", videoIds);
        wrapper.orderByDesc("update_time");
        wrapper.last("limit 1");
        List<VideoPlayHistory> histories = videoPlayHistoryRepository.list(wrapper);
        return histories.isEmpty() ? null : histories.get(0);
    }

    private VideoPlayHistory getPlayProgressByVideoId(Long videoId, Long userId) {
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        return videoPlayHistoryRepository.getOne(wrapper);
    }

    public List<VideoPlayHistoryItem> getRecentPlayList(Long userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        int fetchSize = Math.min(size * 3, 300);
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("update_time");
        wrapper.last("limit " + fetchSize);
        List<VideoPlayHistory> histories = videoPlayHistoryRepository.list(wrapper);
        if (histories == null || histories.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> videoIds = histories.stream()
                .map(VideoPlayHistory::getVideoId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Video> videoMap = videoIds.isEmpty()
                ? Collections.emptyMap()
                : videoRepository.listByIds(videoIds).stream()
                .collect(Collectors.toMap(Video::getId, v -> v, (a, b) -> a));

        List<VideoEpisode> episodes = videoIds.isEmpty()
                ? Collections.emptyList()
                : videoEpisodeRepository.list(new QueryWrapper<VideoEpisode>().in("video_id", videoIds));
        Map<Long, Long> videoToCollection = episodes.stream()
                .filter(e -> e.getVideoId() != null && e.getCollectionId() != null)
                .collect(Collectors.toMap(VideoEpisode::getVideoId, VideoEpisode::getCollectionId, (a, b) -> a));
        Set<Long> collectionIds = new HashSet<>(videoToCollection.values());
        Map<Long, VideoCollection> collectionMap = collectionIds.isEmpty()
                ? Collections.emptyMap()
                : videoCollectionRepository.listByIds(collectionIds).stream()
                .collect(Collectors.toMap(VideoCollection::getId, c -> c, (a, b) -> a));
        fillCollectionPlayCounts(new ArrayList<>(collectionMap.values()));

        List<VideoPlayHistoryItem> items = new ArrayList<>();
        Set<Long> seenCollections = new HashSet<>();
        Set<Long> seenVideos = new HashSet<>();
        for (VideoPlayHistory history : histories) {
            if (items.size() >= size) {
                break;
            }
            Long videoId = history.getVideoId();
            if (videoId == null) {
                continue;
            }
            Long collectionId = videoToCollection.get(videoId);
            if (collectionId != null) {
                if (!seenCollections.add(collectionId)) {
                    continue;
                }
                VideoCollection collection = collectionMap.get(collectionId);
                if (collection == null) {
                    continue;
                }
                normalizeCollectionCoverUrl(collection);
                VideoPlayHistoryItem item = new VideoPlayHistoryItem();
                item.setHistory(history);
                item.setItemType("collection");
                item.setCollection(collection);
                items.add(item);
                continue;
            }

            if (!seenVideos.add(videoId)) {
                continue;
            }
            Video video = videoMap.get(videoId);
            if (video == null) {
                continue;
            }
            normalizeVideoCoverUrl(video);
            VideoPlayHistoryItem item = new VideoPlayHistoryItem();
            item.setHistory(history);
            item.setItemType("video");
            item.setVideo(video);
            items.add(item);
        }
        return items;
    }

    private void fillCollectionPlayCounts(List<VideoCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return;
        }
        List<Long> collectionIds = collections.stream()
                .map(VideoCollection::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (collectionIds.isEmpty()) {
            return;
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(
                new QueryWrapper<VideoEpisode>().in("collection_id", collectionIds)
        );
        if (episodes == null || episodes.isEmpty()) {
            for (VideoCollection collection : collections) {
                collection.setPlayCount(0L);
                collection.setLikeCount(0L);
                collection.setCollectCount(0L);
            }
            return;
        }

        Set<Long> videoIds = episodes.stream()
                .map(VideoEpisode::getVideoId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Video> videoMap = videoIds.isEmpty()
                ? Collections.emptyMap()
                : videoRepository.listByIds(videoIds).stream()
                .filter(video -> video.getId() != null)
                .collect(Collectors.toMap(Video::getId, video -> video, (a, b) -> a));

        Map<Long, Long> collectionPlayMap = new HashMap<>();
        Map<Long, Long> collectionLikeMap = new HashMap<>();
        Map<Long, Long> collectionCollectMap = new HashMap<>();
        for (VideoEpisode episode : episodes) {
            Long collectionId = episode.getCollectionId();
            Long videoId = episode.getVideoId();
            if (collectionId == null || videoId == null) {
                continue;
            }
            Video video = videoMap.get(videoId);
            Long playCount = video != null && video.getPlayCount() != null ? video.getPlayCount() : 0L;
            Long likeCount = video != null && video.getLikeCount() != null ? video.getLikeCount() : 0L;
            Long collectCount = video != null && video.getCollectCount() != null ? video.getCollectCount() : 0L;
            collectionPlayMap.merge(collectionId, playCount, Long::sum);
            collectionLikeMap.merge(collectionId, likeCount, Long::sum);
            collectionCollectMap.merge(collectionId, collectCount, Long::sum);
        }

        for (VideoCollection collection : collections) {
            Long id = collection.getId();
            if (id == null) {
                continue;
            }
            collection.setPlayCount(collectionPlayMap.getOrDefault(id, 0L));
            collection.setLikeCount(collectionLikeMap.getOrDefault(id, 0L));
            collection.setCollectCount(collectionCollectMap.getOrDefault(id, 0L));
        }
    }

    private void normalizeVideoCoverUrl(Video video) {
        if (video == null || video.getId() == null) {
            return;
        }
        String coverUrl = video.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            video.setCoverUrl("/api/cover/video/" + video.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        video.setCoverUrl("/api/cover/video/" + video.getId());
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void normalizeCollectionCoverUrl(VideoCollection collection) {
        if (collection == null || collection.getId() == null) {
            return;
        }
        String coverUrl = collection.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            collection.setCoverUrl("/api/cover/collection/" + collection.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        collection.setCoverUrl("/api/cover/collection/" + collection.getId());
    }
}
