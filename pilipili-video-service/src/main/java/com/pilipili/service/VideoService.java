package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.in.VideoCondition;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.VideoRepository;
import com.pilipili.utils.FileUploadUtil;
import com.pilipili.utils.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * 视频服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoService {

    private final VideoRepository videoRepository;
    private final StorageService storageService;

    /**
     * 上传视频
     */
    @Transactional(rollbackFor = Exception.class)
    public Video uploadVideo(MultipartFile videoFile, MultipartFile coverFile, Video video, User user) {
        // 验证视频格式
        if (!FileUploadUtil.isValidVideoFormat(videoFile.getOriginalFilename())) {
            throw new BusinessException(Status.FILE_FORMAT_ERROR, "不支持的视频格式");
        }

        // 验证文件大小（默认限制500MB）
        long maxSize = 500 * 1024 * 1024L;
        if (videoFile.getSize() > maxSize) {
            throw new BusinessException(Status.FILE_SIZE_ERROR, "视频文件大小超过限制");
        }

        // 上传视频文件
        String videoUrl = storageService.uploadFile(videoFile, "videos");
        video.setVideoUrl(videoUrl);
        video.setFileSize(videoFile.getSize());
        video.setFormat(FileUploadUtil.getFileExtension(videoFile.getOriginalFilename()));

        // 上传封面
        if (coverFile != null && !coverFile.isEmpty()) {
            if (!FileUploadUtil.isValidImageFormat(coverFile.getOriginalFilename())) {
                throw new BusinessException(Status.FILE_FORMAT_ERROR, "不支持的封面格式");
            }
            String coverUrl = storageService.uploadFile(coverFile, "covers");
            video.setCoverUrl(coverUrl);
        }

        // 设置视频信息
        video.setUserId(user.getId());
        video.setUserName(user.getUsername());
        video.setStatus(0); // 待审核
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCollectCount(0L);
        video.setCommentCount(0L);
        video.setCreateId(user.getId());
        video.setCreateName(user.getUsername());
        video.setCreateTime(new Date());
        video.setLogicDel(0);

        videoRepository.save(video);
        log.info("视频上传成功: videoId={}, userId={}", video.getId(), user.getId());
        return video;
    }

    /**
     * 分片上传视频
     */
    public String uploadVideoChunk(MultipartFile chunk, String folder, Integer chunkNumber, Integer totalChunks, String uploadId) {
        return storageService.uploadChunk(chunk, folder, chunkNumber, totalChunks, uploadId);
    }

    /**
     * 完成分片上传
     */
    @Transactional(rollbackFor = Exception.class)
    public Video completeVideoUpload(String folder, String uploadId, String filename, Video video, User user) {
        String videoUrl = storageService.completeChunkUpload(folder, uploadId, filename);
        video.setVideoUrl(videoUrl);
        video.setUserId(user.getId());
        video.setUserName(user.getUsername());
        video.setStatus(0);
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCollectCount(0L);
        video.setCommentCount(0L);
        video.setCreateId(user.getId());
        video.setCreateName(user.getUsername());
        video.setCreateTime(new Date());
        video.setLogicDel(0);

        videoRepository.save(video);
        log.info("分片上传完成: videoId={}, userId={}", video.getId(), user.getId());
        return video;
    }

    /**
     * 更新视频信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Video updateVideo(Video video, User user) {
        Video existingVideo = videoRepository.getById(video.getId());
        if (existingVideo == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "视频不存在");
        }

        // 只有上传者或管理员可以修改
        if (!existingVideo.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BusinessException(Status.FORBIDDEN, "无权限修改此视频");
        }

        video.setUpdateId(user.getId());
        video.setUpdateName(user.getUsername());
        video.setUpdateTime(new Date());
        videoRepository.updateById(video);
        log.info("视频信息更新成功: videoId={}", video.getId());
        return video;
    }

    /**
     * 删除视频
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(Long videoId, User user) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "视频不存在");
        }

        // 只有上传者或管理员可以删除
        if (!video.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new BusinessException(Status.FORBIDDEN, "无权限删除此视频");
        }

        videoRepository.removeById(videoId);
        // 删除文件
        if (video.getVideoUrl() != null) {
            storageService.deleteFile(video.getVideoUrl());
        }
        if (video.getCoverUrl() != null) {
            storageService.deleteFile(video.getCoverUrl());
        }
        log.info("视频删除成功: videoId={}", videoId);
    }

    /**
     * 分页查询视频
     */
    public Page<Video> getVideoPage(VideoCondition condition, Integer pageNum, Integer pageSize) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();

        if (condition.getTitle() != null && !condition.getTitle().isEmpty()) {
            wrapper.like("title", condition.getTitle());
        }
        if (condition.getCategoryId() != null) {
            wrapper.eq("category_id", condition.getCategoryId());
        }
        if (condition.getUserId() != null) {
            wrapper.eq("user_id", condition.getUserId());
        }
        if (condition.getStatus() != null) {
            wrapper.eq("status", condition.getStatus());
        }
        if (condition.getTags() != null && !condition.getTags().isEmpty()) {
            wrapper.like("tags", condition.getTags());
        }

        wrapper.orderByDesc("create_time");
        return videoRepository.page(page, wrapper);
    }

    /**
     * 根据ID获取视频
     */
    public Video getVideoById(Long videoId) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "视频不存在");
        }
        return video;
    }

    /**
     * 更新视频状态（审核/上线/下架）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVideoStatus(Long videoId, Integer status, String auditRemark, User user) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "视频不存在");
        }

        video.setStatus(status);
        video.setAuditRemark(auditRemark);
        video.setUpdateId(user.getId());
        video.setUpdateName(user.getUsername());
        video.setUpdateTime(new Date());
        videoRepository.updateById(video);
        log.info("视频状态更新成功: videoId={}, status={}", videoId, status);
    }

    /**
     * 增加播放量
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementPlayCount(Long videoId) {
        Video video = videoRepository.getById(videoId);
        if (video != null) {
            video.setPlayCount(video.getPlayCount() + 1);
            videoRepository.updateById(video);
        }
    }
}
