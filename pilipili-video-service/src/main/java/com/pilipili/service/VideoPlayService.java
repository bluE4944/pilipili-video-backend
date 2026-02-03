package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.VideoPlayHistory;
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
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        return videoPlayHistoryRepository.getOne(wrapper);
    }
}
