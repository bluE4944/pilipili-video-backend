package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollect;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoComment;
import com.pilipili.entity.VideoLike;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.entity.out.UserBehaviorStats;
import com.pilipili.repository.VideoCollectRepository;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoCommentRepository;
import com.pilipili.repository.VideoLikeRepository;
import com.pilipili.repository.VideoPlayHistoryRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final VideoLikeRepository videoLikeRepository;
    private final VideoCollectRepository videoCollectRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final VideoCollectionRepository videoCollectionRepository;

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
    public UserBehaviorStats getUserBehaviorAnalysis(Long userId) {
        UserBehaviorStats stats = new UserBehaviorStats();

        QueryWrapper<VideoPlayHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<VideoPlayHistory> histories = videoPlayHistoryRepository.list(wrapper);

        int totalWatchTime = histories.stream()
                .mapToInt(history -> history.getPlayDuration() == null ? 0 : history.getPlayDuration())
                .sum();
        stats.setTotalWatchTime(totalWatchTime);

        Set<Long> watchedVideoIds = histories.stream()
                .map(VideoPlayHistory::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        stats.setWatchedVideoCount(watchedVideoIds.size());

        long playCount = histories.size();
        stats.setTotalPlayCount(playCount);
        stats.setPlayCount(playCount);

        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
        Instant startOfWeek = LocalDate.now(zoneId)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(zoneId)
                .toInstant();
        Instant startOfMonth = LocalDate.now(zoneId)
                .withDayOfMonth(1)
                .atStartOfDay(zoneId)
                .toInstant();

        long todayPlayCount = 0;
        long weekPlayCount = 0;
        long monthPlayCount = 0;
        Date lastPlayTime = null;

        for (VideoPlayHistory history : histories) {
            Date time = history.getUpdateTime() != null ? history.getUpdateTime() : history.getCreateTime();
            if (time == null) {
                continue;
            }
            if (lastPlayTime == null || time.after(lastPlayTime)) {
                lastPlayTime = time;
            }
            Instant instant = time.toInstant();
            if (!instant.isBefore(startOfDay)) {
                todayPlayCount++;
            }
            if (!instant.isBefore(startOfWeek)) {
                weekPlayCount++;
            }
            if (!instant.isBefore(startOfMonth)) {
                monthPlayCount++;
            }
        }

        stats.setTodayPlayCount(todayPlayCount);
        stats.setWeekPlayCount(weekPlayCount);
        stats.setMonthPlayCount(monthPlayCount);
        stats.setLastPlayTime(lastPlayTime);

        QueryWrapper<VideoLike> likeWrapper = new QueryWrapper<>();
        likeWrapper.eq("user_id", userId);
        likeWrapper.eq("is_like", 1);
        stats.setLikeCount(videoLikeRepository.count(likeWrapper));

        QueryWrapper<VideoComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("user_id", userId);
        stats.setCommentCount(videoCommentRepository.count(commentWrapper));

        QueryWrapper<VideoCollect> collectWrapper = new QueryWrapper<>();
        collectWrapper.eq("user_id", userId);
        stats.setCollectCount(videoCollectRepository.count(collectWrapper));

        QueryWrapper<VideoCollection> collectionWrapper = new QueryWrapper<>();
        collectionWrapper.eq("create_id", userId);
        stats.setFavoriteCount(videoCollectionRepository.count(collectionWrapper));

        QueryWrapper<VideoPlayHistory> recentWrapper = new QueryWrapper<>();
        recentWrapper.eq("user_id", userId);
        recentWrapper.orderByDesc("update_time");
        recentWrapper.last("LIMIT 10");
        List<VideoPlayHistory> recentHistories = videoPlayHistoryRepository.list(recentWrapper);

        if (recentHistories == null || recentHistories.isEmpty()) {
            stats.setRecentWatchedVideos(Collections.emptyList());
        } else {
            List<Long> recentVideoIds = recentHistories.stream()
                    .map(VideoPlayHistory::getVideoId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, String> titleMap = recentVideoIds.isEmpty()
                    ? Collections.emptyMap()
                    : videoRepository.listByIds(recentVideoIds).stream()
                    .filter(video -> video.getId() != null)
                    .collect(Collectors.toMap(
                            Video::getId,
                            video -> video.getTitle() == null ? String.valueOf(video.getId()) : video.getTitle(),
                            (a, b) -> a
                    ));

            List<String> recentTitles = new ArrayList<>();
            for (VideoPlayHistory history : recentHistories) {
                Long videoId = history.getVideoId();
                if (videoId == null) {
                    continue;
                }
                String title = titleMap.get(videoId);
                if (title != null && !title.isEmpty()) {
                    recentTitles.add(title);
                }
            }
            stats.setRecentWatchedVideos(recentTitles);
        }

        return stats;
    }

    /**
     * 获取热门视频排行
     */
    public List<Video> getHotVideoRanking(Integer limit) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1); // 只统计已上线的视频
        wrapper.orderByDesc("play_count", "like_count");
        wrapper.last("LIMIT " + limit);
        List<Video> videos = videoRepository.list(wrapper);
        normalizeListCoverUrl(videos);
        return videos;
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

    private void normalizeListCoverUrl(List<Video> videos) {
        if (videos == null) {
            return;
        }
        for (Video video : videos) {
            normalizeVideoCoverUrl(video);
        }
    }

    private void normalizeVideoCoverUrl(Video video) {
        if (video == null || video.getId() == null) {
            return;
        }
        String coverUrl = video.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            video.setCoverUrl("/api/cover/video/" + video.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        video.setCoverUrl("/api/cover/video/" + video.getId());
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
