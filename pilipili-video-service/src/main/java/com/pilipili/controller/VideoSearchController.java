package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.Video;
import com.pilipili.entity.out.Result;
import com.pilipili.service.VideoSearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频搜索控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/search")
@Api(value = "视频搜索控制器", tags = "视频搜索")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoSearchController {

    private final VideoSearchService videoSearchService;

    /**
     * 关键词搜索
     */
    @GetMapping("/keyword")
    @ApiOperation("关键词搜索")
    public Result<Page<Video>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Video> page = videoSearchService.searchByKeyword(keyword, pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 分类搜索
     */
    @GetMapping("/category/{categoryId}")
    @ApiOperation("分类搜索")
    public Result<Page<Video>> searchByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Video> page = videoSearchService.searchByCategory(categoryId, pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 标签搜索
     */
    @GetMapping("/tag")
    @ApiOperation("标签搜索")
    public Result<Page<Video>> searchByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Video> page = videoSearchService.searchByTag(tag, pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 相关视频推荐
     */
    @GetMapping("/related/{videoId}")
    @ApiOperation("相关视频推荐")
    public Result<List<Video>> getRelatedVideos(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Video> videos = videoSearchService.getRelatedVideos(videoId, limit);
        return Result.build(videos);
    }

    /**
     * 热门视频推荐
     */
    @GetMapping("/hot")
    @ApiOperation("热门视频推荐")
    public Result<List<Video>> getHotVideos(@RequestParam(defaultValue = "10") Integer limit) {
        List<Video> videos = videoSearchService.getHotVideos(limit);
        return Result.build(videos);
    }
}
