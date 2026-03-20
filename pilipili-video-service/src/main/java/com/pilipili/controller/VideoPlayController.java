package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.entity.out.Result;
import com.pilipili.entity.out.VideoPlayHistoryItem;
import com.pilipili.entity.out.VideoPlaySourceInfo;
import com.pilipili.service.VideoService;
import com.pilipili.service.VideoPlayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 视频播放控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/play")
@Api(value = "视频播放控制器", tags = "视频播放")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoPlayController {

    private final VideoPlayService videoPlayService;
    private final VideoService videoService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null; // 允许未登录用户播放
    }

    /**
     * 获取视频播放地址
     */
    @GetMapping("/url/{videoId}")
    @ApiOperation("获取视频播放地址")
    public Result<String> getVideoPlayUrl(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            @ApiParam(value = "过期时间（秒）", example = "3600") @RequestParam(defaultValue = "3600") Long expireSeconds) {
        String playUrl = videoPlayService.getVideoPlayUrl(videoId, expireSeconds);
        return Result.build(playUrl);
    }

    /**
     * 获取视频播放源信息
     */
    @GetMapping("/source/{videoId}")
    @ApiOperation("获取视频播放源信息")
    public Result<VideoPlaySourceInfo> getVideoPlaySourceInfo(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            @ApiParam(value = "过期时间（秒）", example = "3600") @RequestParam(defaultValue = "3600") Long expireSeconds) {
        VideoPlaySourceInfo sourceInfo = videoPlayService.getVideoPlaySourceInfo(videoId, expireSeconds);
        return Result.build(sourceInfo);
    }

    /**
     * 增加播放量
     */
    @PostMapping("/count/{videoId}")
    @ApiOperation("增加播放量")
    public Result<Long> incrementPlayCount(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId) {
        Long playCount = videoService.incrementPlayCount(videoId);
        return Result.build(playCount);
    }

    /**
     * 记录播放进度
     */
    @PostMapping("/progress")
    @ApiOperation("记录播放进度")
    public Result<?> recordPlayProgress(
            @ApiParam(value = "视频ID", required = true, example = "1") @RequestParam Long videoId,
            @ApiParam(value = "播放进度（秒）", required = true, example = "100") @RequestParam Integer progress,
            @ApiParam(value = "播放时长（秒）", example = "120") @RequestParam(required = false) Integer playDuration,
            @ApiParam(value = "清晰度：1-标清，2-高清，3-超清", example = "2") @RequestParam(required = false) Integer quality,
            @ApiParam(value = "播放倍速", example = "1.0") @RequestParam(required = false) Double playbackRate) {
        User user = getCurrentUser();
        if (user != null) {
            videoPlayService.recordPlayProgress(videoId, user.getId(), progress, playDuration, quality, playbackRate);
        }
        return Result.build();
    }

    /**
     * 获取播放进度
     */
    @GetMapping("/progress/{videoId}")
    @ApiOperation("获取播放进度")
    public Result<VideoPlayHistory> getPlayProgress(@ApiParam(value = "视频ID或合集ID", required = true, example = "1") @PathVariable Long videoId) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.build(null);
        }
        VideoPlayHistory history = videoPlayService.getPlayProgress(videoId, user.getId());
        return Result.build(history);
    }

    /**
     * 最近播放列表
     */
    @GetMapping("/recent")
    @ApiOperation("获取最近播放列表")
    public Result<List<VideoPlayHistoryItem>> getRecentPlayList(
            @ApiParam(value = "数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.build(Collections.emptyList());
        }
        List<VideoPlayHistoryItem> items = videoPlayService.getRecentPlayList(user.getId(), size);
        return Result.build(items);
    }
}
