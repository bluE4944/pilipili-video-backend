package com.pilipili.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.out.Result;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return Result.build(videoCollectionRepository.page(page, wrapper));
    }

    /**
     * 根据ID获取合集详情
     */
    @GetMapping("/{collectionId}")
    @ApiOperation("根据ID获取合集详情")
    public Result<VideoCollection> getCollectionById(@PathVariable Long collectionId) {
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
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
}
