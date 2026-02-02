package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.in.VideoCondition;
import com.pilipili.entity.out.Result;
import com.pilipili.service.VideoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video")
@Api(value = "视频控制器", tags = "视频管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoController {

    private final VideoService videoService;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("用户未登录");
    }

    /**
     * 上传视频
     */
    @PostMapping("/upload")
    @ApiOperation("上传视频")
    public Result<Video> uploadVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @ModelAttribute Video video) {
        User user = getCurrentUser();
        Video result = videoService.uploadVideo(videoFile, coverFile, video, user);
        return Result.build(result);
    }

    /**
     * 分片上传视频
     */
    @PostMapping("/upload/chunk")
    @ApiOperation("分片上传视频")
    public Result<String> uploadVideoChunk(
            @ApiParam(value = "文件分片", required = true) @RequestParam("chunk") MultipartFile chunk,
            @ApiParam(value = "文件夹路径", required = true, example = "videos") @RequestParam("folder") String folder,
            @ApiParam(value = "分片序号", required = true, example = "1") @RequestParam("chunkNumber") Integer chunkNumber,
            @ApiParam(value = "总分片数", required = true, example = "10") @RequestParam("totalChunks") Integer totalChunks,
            @ApiParam(value = "上传ID", required = true, example = "upload-123") @RequestParam("uploadId") String uploadId) {
        String result = videoService.uploadVideoChunk(chunk, folder, chunkNumber, totalChunks, uploadId);
        return Result.build(result);
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/upload/complete")
    @ApiOperation("完成分片上传")
    public Result<Video> completeVideoUpload(
            @RequestParam("folder") String folder,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("filename") String filename,
            @ModelAttribute Video video) {
        User user = getCurrentUser();
        Video result = videoService.completeVideoUpload(folder, uploadId, filename, video, user);
        return Result.build(result);
    }

    /**
     * 更新视频信息
     */
    @PutMapping("/{videoId}")
    @ApiOperation("更新视频信息")
    public Result<Video> updateVideo(@PathVariable Long videoId, @RequestBody Video video) {
        video.setId(videoId);
        User user = getCurrentUser();
        Video result = videoService.updateVideo(video, user);
        return Result.build(result);
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/{videoId}")
    @ApiOperation("删除视频")
    public Result<?> deleteVideo(@PathVariable Long videoId) {
        User user = getCurrentUser();
        videoService.deleteVideo(videoId, user);
        return Result.build();
    }

    /**
     * 分页查询视频
     */
    @GetMapping("/page")
    @ApiOperation("分页查询视频")
    public Result<Page<Video>> getVideoPage(
            @ModelAttribute VideoCondition condition,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Video> page = videoService.getVideoPage(condition, pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 根据ID获取视频
     */
    @GetMapping("/{videoId}")
    @ApiOperation("根据ID获取视频")
    public Result<Video> getVideoById(@PathVariable Long videoId) {
        Video video = videoService.getVideoById(videoId);
        return Result.build(video);
    }

    /**
     * 更新视频状态（审核/上线/下架）
     */
    @PutMapping("/{videoId}/status")
    @ApiOperation("更新视频状态")
    public Result<?> updateVideoStatus(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            @ApiParam(value = "视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过", required = true, example = "1") @RequestParam Integer status,
            @ApiParam(value = "审核意见", example = "审核通过") @RequestParam(required = false) String auditRemark) {
        User user = getCurrentUser();
        videoService.updateVideoStatus(videoId, status, auditRemark, user);
        return Result.build();
    }
}
