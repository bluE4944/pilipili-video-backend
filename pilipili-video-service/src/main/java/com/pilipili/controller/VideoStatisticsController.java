package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.out.Result;
import com.pilipili.service.VideoStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频统计控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/statistics")
@Api(value = "视频统计控制器", tags = "视频统计")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoStatisticsController {

    private final VideoStatisticsService videoStatisticsService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取视频播放量统计
     */
    @GetMapping("/video/{videoId}")
    @ApiOperation("获取视频播放量统计")
    public Result<Map<String, Object>> getVideoPlayStatistics(@PathVariable Long videoId) {
        Map<String, Object> statistics = videoStatisticsService.getVideoPlayStatistics(videoId);
        return Result.build(statistics);
    }

    /**
     * 获取用户行为分析
     */
    @GetMapping("/user/behavior")
    @ApiOperation("获取用户行为分析")
    public Result<Map<String, Object>> getUserBehaviorAnalysis() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.build(com.pilipili.utils.Status.UNAUTHORIZED, "用户未登录");
        }
        Map<String, Object> analysis = videoStatisticsService.getUserBehaviorAnalysis(user.getId());
        return Result.build(analysis);
    }

    /**
     * 获取热门视频排行
     */
    @GetMapping("/hot/ranking")
    @ApiOperation("获取热门视频排行")
    public Result<List<Video>> getHotVideoRanking(@RequestParam(defaultValue = "10") Integer limit) {
        List<Video> videos = videoStatisticsService.getHotVideoRanking(limit);
        return Result.build(videos);
    }

    /**
     * 获取视频播放趋势
     */
    @GetMapping("/video/{videoId}/trend")
    @ApiOperation("获取视频播放趋势")
    public Result<Map<String, Long>> getVideoPlayTrend(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Long> trend = videoStatisticsService.getVideoPlayTrend(videoId, days);
        return Result.build(trend);
    }
}
