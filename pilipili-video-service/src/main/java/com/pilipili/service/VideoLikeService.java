package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoLike;
import com.pilipili.repository.VideoLikeRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 视频点赞服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoLikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoRepository videoRepository;

    /**
     * 点赞/取消点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleLike(Long videoId, Long userId, Integer isLike) {
        QueryWrapper<VideoLike> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        VideoLike videoLike = videoLikeRepository.getOne(wrapper);

        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        if (videoLike == null) {
            // 创建新记录
            videoLike = new VideoLike();
            videoLike.setVideoId(videoId);
            videoLike.setUserId(userId);
            videoLike.setIsLike(isLike);
            videoLike.setCreateTime(new Date());
            videoLike.setLogicDel(0);
            videoLikeRepository.save(videoLike);
        } else {
            // 更新记录
            videoLike.setIsLike(isLike);
            videoLike.setUpdateTime(new Date());
            videoLikeRepository.updateById(videoLike);
        }

        // 更新视频点赞数
        QueryWrapper<VideoLike> countWrapper = new QueryWrapper<>();
        countWrapper.eq("video_id", videoId);
        countWrapper.eq("is_like", 1);
        long likeCount = videoLikeRepository.count(countWrapper);
        video.setLikeCount(likeCount);
        videoRepository.updateById(video);

        log.info("视频点赞操作成功: videoId={}, userId={}, isLike={}", videoId, userId, isLike);
    }

    /**
     * 检查用户是否已点赞
     */
    public boolean isLiked(Long videoId, Long userId) {
        QueryWrapper<VideoLike> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        wrapper.eq("is_like", 1);
        return videoLikeRepository.count(wrapper) > 0;
    }
}
