package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollect;
import com.pilipili.repository.VideoCollectRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 视频收藏服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoCollectService {

    private final VideoCollectRepository videoCollectRepository;
    private final VideoRepository videoRepository;

    /**
     * 收藏视频
     */
    @Transactional(rollbackFor = Exception.class)
    public void collectVideo(Long videoId, Long userId, String folderName) {
        QueryWrapper<VideoCollect> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        VideoCollect existing = videoCollectRepository.getOne(wrapper);

        if (existing != null) {
            throw new RuntimeException("视频已收藏");
        }

        VideoCollect videoCollect = new VideoCollect();
        videoCollect.setVideoId(videoId);
        videoCollect.setUserId(userId);
        videoCollect.setFolderName(folderName != null ? folderName : "默认收藏夹");
        videoCollect.setCreateTime(new Date());
        videoCollect.setLogicDel(0);
        videoCollectRepository.save(videoCollect);

        // 更新视频收藏数
        Video video = videoRepository.getById(videoId);
        if (video != null) {
            QueryWrapper<VideoCollect> countWrapper = new QueryWrapper<>();
            countWrapper.eq("video_id", videoId);
            long collectCount = videoCollectRepository.count(countWrapper);
            video.setCollectCount(collectCount);
            videoRepository.updateById(video);
        }

        log.info("视频收藏成功: videoId={}, userId={}", videoId, userId);
    }

    /**
     * 取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelCollect(Long videoId, Long userId) {
        QueryWrapper<VideoCollect> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        videoCollectRepository.remove(wrapper);

        // 更新视频收藏数
        Video video = videoRepository.getById(videoId);
        if (video != null) {
            QueryWrapper<VideoCollect> countWrapper = new QueryWrapper<>();
            countWrapper.eq("video_id", videoId);
            long collectCount = videoCollectRepository.count(countWrapper);
            video.setCollectCount(collectCount);
            videoRepository.updateById(video);
        }

        log.info("取消收藏成功: videoId={}, userId={}", videoId, userId);
    }

    /**
     * 分页查询用户收藏
     */
    public Page<VideoCollect> getUserCollects(Long userId, Integer pageNum, Integer pageSize) {
        Page<VideoCollect> page = new Page<>(pageNum, pageSize);
        QueryWrapper<VideoCollect> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        return videoCollectRepository.page(page, wrapper);
    }

    /**
     * 检查用户是否已收藏
     */
    public boolean isCollected(Long videoId, Long userId) {
        QueryWrapper<VideoCollect> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.eq("user_id", userId);
        return videoCollectRepository.count(wrapper) > 0;
    }
}
