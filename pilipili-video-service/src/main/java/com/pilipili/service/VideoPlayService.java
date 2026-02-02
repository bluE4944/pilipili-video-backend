package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.repository.VideoPlayHistoryRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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
    private final VideoPlayHistoryRepository videoPlayHistoryRepository;
    private final StorageService storageService;

    /**
     * 获取视频播放地址（带防盗链和时效控制）
     */
    public String getVideoPlayUrl(Long videoId, Long expireSeconds) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        // 生成临时访问URL
        return storageService.generatePresignedUrl(video.getVideoUrl(), expireSeconds);
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
