package com.pilipili.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.out.Result;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.repository.VideoRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频合集控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/collection")
@Api(value = "视频合集控制器", tags = "视频合集管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoCollectionController {

    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoRepository videoRepository;

    /**
     * 分页查询合集
     */
    @GetMapping("/page")
    @ApiOperation("分页查询合集")
    public Result<Page<VideoCollection>> getCollectionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer collectionType) {
        Page<VideoCollection> page = new Page<>(pageNum, pageSize);
        QueryWrapper<VideoCollection> wrapper = new QueryWrapper<>();
        if (collectionType != null) {
            wrapper.eq("collection_type", collectionType);
        }
        wrapper.eq("enabled", 1);
        wrapper.orderByDesc("create_time");
        Page<VideoCollection> result = videoCollectionRepository.page(page, wrapper);
        if (result.getRecords() != null) {
            fillCollectionPlayCounts(result.getRecords());
            for (VideoCollection collection : result.getRecords()) {
                normalizeCollectionCoverUrl(collection);
            }
        }
        return Result.build(result);
    }

    /**
     * 根据ID获取合集详情
     */
    @GetMapping("/{collectionId}")
    @ApiOperation("根据ID获取合集详情")
    public Result<VideoCollection> getCollectionById(@PathVariable Long collectionId) {
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        fillCollectionPlayCount(collection);
        normalizeCollectionCoverUrl(collection);
        return Result.build(collection);
    }

    /**
     * 获取合集的视频分集列表
     */
    @GetMapping("/{collectionId}/episodes")
    @ApiOperation("获取合集的视频分集列表")
    public Result<List<VideoEpisode>> getEpisodes(@PathVariable Long collectionId) {
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        wrapper.orderByAsc("sort_order", "episode_number");
        List<VideoEpisode> episodes = videoEpisodeRepository.list(wrapper);
        return Result.build(episodes);
    }

    /**
     * 更新合集信息
     */
    @PutMapping("/{collectionId}")
    @ApiOperation("更新合集信息")
    public Result<VideoCollection> updateCollection(@PathVariable Long collectionId, @RequestBody VideoCollection collection) {
        collection.setId(collectionId);
        videoCollectionRepository.updateById(collection);
        normalizeCollectionCoverUrl(collection);
        return Result.build(collection);
    }

    /**
     * 删除合集
     */
    @DeleteMapping("/{collectionId}")
    @ApiOperation("删除合集")
    public Result<?> deleteCollection(@PathVariable Long collectionId) {
        videoCollectionRepository.removeById(collectionId);
        // 同时删除所有分集
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        videoEpisodeRepository.remove(wrapper);
        return Result.build();
    }
    private void normalizeCollectionCoverUrl(VideoCollection collection) {
        if (collection == null || collection.getId() == null) {
            return;
        }
        String coverUrl = collection.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            collection.setCoverUrl("/api/cover/collection/" + collection.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        collection.setCoverUrl("/api/cover/collection/" + collection.getId());
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void fillCollectionPlayCount(VideoCollection collection) {
        if (collection == null || collection.getId() == null) {
            return;
        }
        fillCollectionPlayCounts(java.util.Collections.singletonList(collection));
    }

    private void fillCollectionPlayCounts(List<VideoCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return;
        }
        List<Long> collectionIds = collections.stream()
                .map(VideoCollection::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (collectionIds.isEmpty()) {
            return;
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(
                new QueryWrapper<VideoEpisode>().in("collection_id", collectionIds)
        );
        if (episodes == null || episodes.isEmpty()) {
            for (VideoCollection collection : collections) {
                collection.setPlayCount(0L);
                collection.setLikeCount(0L);
                collection.setCollectCount(0L);
            }
            return;
        }

        Set<Long> videoIds = episodes.stream()
                .map(VideoEpisode::getVideoId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Video> videoMap = videoIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : videoRepository.listByIds(videoIds).stream()
                .filter(video -> video.getId() != null)
                .collect(Collectors.toMap(Video::getId, video -> video, (a, b) -> a));

        Map<Long, Long> collectionPlayMap = new HashMap<>();
        Map<Long, Long> collectionLikeMap = new HashMap<>();
        Map<Long, Long> collectionCollectMap = new HashMap<>();
        for (VideoEpisode episode : episodes) {
            Long collectionId = episode.getCollectionId();
            Long videoId = episode.getVideoId();
            if (collectionId == null || videoId == null) {
                continue;
            }
            Video video = videoMap.get(videoId);
            Long playCount = video != null && video.getPlayCount() != null ? video.getPlayCount() : 0L;
            Long likeCount = video != null && video.getLikeCount() != null ? video.getLikeCount() : 0L;
            Long collectCount = video != null && video.getCollectCount() != null ? video.getCollectCount() : 0L;
            collectionPlayMap.merge(collectionId, playCount, Long::sum);
            collectionLikeMap.merge(collectionId, likeCount, Long::sum);
            collectionCollectMap.merge(collectionId, collectCount, Long::sum);
        }

        for (VideoCollection collection : collections) {
            Long id = collection.getId();
            if (id == null) {
                continue;
            }
            collection.setPlayCount(collectionPlayMap.getOrDefault(id, 0L));
            collection.setLikeCount(collectionLikeMap.getOrDefault(id, 0L));
            collection.setCollectCount(collectionCollectMap.getOrDefault(id, 0L));
        }
    }
}
