package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.repository.VideoPlayHistoryRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 视频统计服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoStatisticsService {

    private final VideoRepository videoRepository;
    private final VideoPlayHistoryRepository videoPlayHistoryRepository;

    /**
     * 获取视频播放量统计
     */
    public Map<String, Object> getVideoPlayStatistics(Long videoId) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("videoId", videoId);
        statistics.put("playCount", video.getPlayCount());
        statistics.put("likeCount", video.getLikeCount());
        statistics.put("collectCount", video.getCollectCount());
        statistics.put("commentCount", video.getCommentCount());

        // 计算平均播放时长
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.gt("play_duration", 0);
        List<VideoPlayHistory> histories = videoPlayHistoryRepository.list(wrapper);
        
        if (!histories.isEmpty()) {
            double avgDuration = histories.stream()
                    .mapToInt(VideoPlayHistory::getPlayDuration)
                    .average()
                    .orElse(0.0);
            statistics.put("avgPlayDuration", Math.round(avgDuration));
        } else {
            statistics.put("avgPlayDuration", 0);
        }

        return statistics;
    }

    /**
     * 获取用户行为分析
     */
    public Map<String, Object> getUserBehaviorAnalysis(Long userId) {
        Map<String, Object> analysis = new HashMap<>();

        // 总观看时长
        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<VideoPlayHistory> histories = videoPlayHistoryRepository.list(wrapper);
        
        int totalWatchTime = histories.stream()
                .mapToInt(VideoPlayHistory::getPlayDuration)
                .sum();
        analysis.put("totalWatchTime", totalWatchTime);

        // 观看视频数
        Set<Long> watchedVideoIds = new HashSet<>();
        histories.forEach(h -> watchedVideoIds.add(h.getVideoId()));
        analysis.put("watchedVideoCount", watchedVideoIds.size());

        // 最近观看的视频
        wrapper.orderByDesc("update_time");
        wrapper.last("LIMIT 10");
        List<VideoPlayHistory> recentHistories = videoPlayHistoryRepository.list(wrapper);
        analysis.put("recentWatchedVideos", recentHistories);

        return analysis;
    }

    /**
     * 获取热门视频排行
     */
    public List<Video> getHotVideoRanking(Integer limit) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只统计已上线的视频
        wrapper.orderByDesc("play_count", "like_count");
        wrapper.last("LIMIT " + limit);
        return videoRepository.list(wrapper);
    }

    /**
     * 获取视频播放趋势（按日期统计）
     */
    public Map<String, Long> getVideoPlayTrend(Long videoId, Integer days) {
        // 简化实现，实际应从播放日志表中统计
        Map<String, Long> trend = new HashMap<>();
        Video video = videoRepository.getById(videoId);
        if (video != null) {
            // 这里应该从播放日志表统计，暂时返回总播放量
            trend.put("total", video.getPlayCount());
        }
        return trend;
    }
}
