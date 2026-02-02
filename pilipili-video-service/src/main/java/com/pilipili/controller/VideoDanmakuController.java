package com.pilipili.controller;

import com.pilipili.entity.VideoDanmaku;
import com.pilipili.entity.out.Result;
import com.pilipili.service.VideoDanmakuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频弹幕控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/danmaku")
@Api(value = "视频弹幕控制器", tags = "视频弹幕")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoDanmakuController {

    private final VideoDanmakuService videoDanmakuService;

    /**
     * 添加弹幕
     */
    @PostMapping
    @ApiOperation("添加弹幕")
    public Result<VideoDanmaku> addDanmaku(@RequestBody VideoDanmaku danmaku) {
        VideoDanmaku result = videoDanmakuService.addDanmaku(danmaku);
        return Result.build(result);
    }

    /**
     * 获取视频弹幕列表
     */
    @GetMapping("/{videoId}")
    @ApiOperation("获取视频弹幕列表")
    public Result<List<VideoDanmaku>> getVideoDanmakus(@PathVariable Long videoId) {
        List<VideoDanmaku> danmakus = videoDanmakuService.getVideoDanmakus(videoId);
        return Result.build(danmakus);
    }

    /**
     * 删除弹幕
     */
    @DeleteMapping("/{danmakuId}")
    @ApiOperation("删除弹幕")
    public Result<?> deleteDanmaku(@PathVariable Long danmakuId) {
        videoDanmakuService.deleteDanmaku(danmakuId);
        return Result.build();
    }
}
