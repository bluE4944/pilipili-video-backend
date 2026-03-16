package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.entity.out.VideoPlayHistoryItem;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 视频播放服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoPlayService {

    private final VideoRepository videoRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoPlayHistoryRepository videoPlayHistoryRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 获取视频播放地址（带防盗链和时效控制）
     */
    public String getVideoPlayUrl(Long videoId, Long expireSeconds) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        long expireAt = expireSeconds != null ? (System.currentTimeMillis() / 1000 + expireSeconds) : 0L;
        if (expireAt > 0) {
            return "/api/video/stream/" + videoId + "?expire=" + expireAt;
        }
        return "/api/video/stream/" + videoId;
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
        Map<Long, Video> videoMap = videoRepository.listByIds(videoIds).stream()
                .collect(Collectors.toMap(Video::getId, v -> v, (a, b) -> a));

        List<VideoEpisode> episodes = videoEpisodeRepository.list(new QueryWrapper<VideoEpisode>().in("video_id", videoIds));
        Map<Long, Long> videoToCollection = episodes.stream()
                .filter(e -> e.getVideoId() != null && e.getCollectionId() != null)
                .collect(Collectors.toMap(VideoEpisode::getVideoId, VideoEpisode::getCollectionId, (a, b) -> a));
        Set<Long> collectionIds = new HashSet<>(videoToCollection.values());
        Map<Long, VideoCollection> collectionMap = collectionIds.isEmpty()
                ? java.util.Collections.emptyMap()
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
                ? java.util.Collections.emptyMap()
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
