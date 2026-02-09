package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.in.BatchVideoUpdateRequest;
import com.pilipili.entity.in.VideoCondition;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.VideoRepository;
import com.pilipili.service.StorageService;
import com.pilipili.service.VideoService;
import com.pilipili.utils.FileUploadUtil;
import com.pilipili.utils.Status;
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
 * 管理员视频管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/videos")
@Api(value = "管理员视频管理", tags = "管理员-视频管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminVideoController {

    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final StorageService storageService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "用户未登录");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询视频")
    public Result<Page<Video>> getVideoPage(
            @ModelAttribute VideoCondition condition,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Video> page = videoService.getVideoPage(condition, pageNum, pageSize);
        return Result.build(page);
    }

    @GetMapping("/{videoId}")
    @ApiOperation("获取视频详情")
    public Result<Video> getVideoById(@PathVariable Long videoId) {
        Video video = videoService.getVideoById(videoId);
        return Result.build(video);
    }

    @PutMapping("/{videoId}")
    @ApiOperation("更新视频信息")
    public Result<Video> updateVideo(@PathVariable Long videoId, @RequestBody Video video) {
        User admin = getCurrentUser();
        video.setId(videoId);
        Video result = videoService.updateVideo(video, admin);
        return Result.build(result);
    }

    @PostMapping("/{videoId}/cover")
    @ApiOperation("上传视频封面")
    public Result<Video> uploadVideoCover(
            @PathVariable Long videoId,
            @RequestParam("coverFile") MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "封面文件不能为空");
        }
        if (!FileUploadUtil.isValidImageFormat(coverFile.getOriginalFilename())) {
            throw new BusinessException(Status.FILE_FORMAT_ERROR, "不支持的封面格式");
        }
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "视频不存在");
        }
        String coverUrl = storageService.uploadFile(coverFile, "covers");
        User admin = getCurrentUser();
        video.setCoverUrl(coverUrl);
        video.setUpdateId(admin.getId());
        video.setUpdateName(admin.getUsername());
        video.setUpdateTime(new java.util.Date());
        videoRepository.updateById(video);
        return Result.build(video);
    }

    @PutMapping("/{videoId}/status")
    @ApiOperation("更新视频状态")
    public Result<?> updateVideoStatus(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            @ApiParam(value = "视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过", required = true, example = "1") @RequestParam Integer status,
            @ApiParam(value = "审核意见", example = "审核通过") @RequestParam(required = false) String auditRemark) {
        User admin = getCurrentUser();
        videoService.updateVideoStatus(videoId, status, auditRemark, admin);
        return Result.build();
    }

    @DeleteMapping("/{videoId}")
    @ApiOperation("删除视频")
    public Result<?> deleteVideo(@PathVariable Long videoId) {
        User admin = getCurrentUser();
        videoService.deleteVideo(videoId, admin);
        return Result.build();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除视频")
    public Result<?> deleteVideos(@RequestBody java.util.List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "视频ID不能为空");
        }
        User admin = getCurrentUser();
        for (Long videoId : videoIds) {
            if (videoId == null) {
                continue;
            }
            videoService.deleteVideo(videoId, admin);
        }
        return Result.build();
    }

    @PutMapping("/batch/status")
    @ApiOperation("批量更新视频状态")
    public Result<?> updateVideoStatusBatch(
            @RequestBody java.util.List<Long> videoIds,
            @ApiParam(value = "视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过", required = true, example = "1")
            @RequestParam Integer status,
            @ApiParam(value = "审核意见", example = "审核通过")
            @RequestParam(required = false) String auditRemark) {
        if (videoIds == null || videoIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "视频ID不能为空");
        }
        User admin = getCurrentUser();
        for (Long videoId : videoIds) {
            if (videoId == null) {
                continue;
            }
            videoService.updateVideoStatus(videoId, status, auditRemark, admin);
        }
        return Result.build();
    }

    @PutMapping("/batch/fields")
    @ApiOperation("批量更新视频标题/分类/标签")
    public Result<?> updateVideoFieldsBatch(@RequestBody BatchVideoUpdateRequest request) {
        if (request == null || request.getVideoIds() == null || request.getVideoIds().isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "视频ID不能为空");
        }
        boolean hasUpdate = false;
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Video> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.in("id", request.getVideoIds());

        if (notBlank(request.getTitle())) {
            wrapper.set("title", request.getTitle());
            hasUpdate = true;
        }
        if (request.getCategoryId() != null) {
            wrapper.set("category_id", request.getCategoryId());
            hasUpdate = true;
        }
        if (request.getCategoryName() != null) {
            wrapper.set("category_name", request.getCategoryName());
            hasUpdate = true;
        }
        if (request.getTags() != null) {
            wrapper.set("tags", request.getTags());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessException(Status.PARAM_ERROR, "至少提供一个更新字段");
        }

        User admin = getCurrentUser();
        wrapper.set("update_id", admin.getId())
                .set("update_name", admin.getUsername())
                .set("update_time", new java.util.Date());
        videoRepository.update(wrapper);
        return Result.build();
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
