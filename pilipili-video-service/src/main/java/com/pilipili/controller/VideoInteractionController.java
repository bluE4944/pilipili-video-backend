package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.VideoCollect;
import com.pilipili.entity.VideoComment;
import com.pilipili.entity.out.Result;
import com.pilipili.service.VideoCollectService;
import com.pilipili.service.VideoCommentService;
import com.pilipili.service.VideoLikeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 视频互动控制器（点赞、收藏、评论）
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/video/interaction")
@Api(value = "视频互动控制器", tags = "视频互动")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoInteractionController {

    private final VideoLikeService videoLikeService;
    private final VideoCollectService videoCollectService;
    private final VideoCommentService videoCommentService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("用户未登录");
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like/{videoId}")
    @ApiOperation("点赞/取消点赞")
    public Result<?> toggleLike(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            @ApiParam(value = "是否点赞：0-取消点赞，1-点赞", required = true, example = "1") @RequestParam Integer isLike) {
        User user = getCurrentUser();
        videoLikeService.toggleLike(videoId, user.getId(), isLike);
        return Result.build();
    }

    /**
     * 检查是否已点赞
     */
    @GetMapping("/like/{videoId}")
    @ApiOperation("检查是否已点赞")
    public Result<Boolean> isLiked(@PathVariable Long videoId) {
        User user = getCurrentUser();
        boolean isLiked = videoLikeService.isLiked(videoId, user.getId());
        return Result.build(isLiked);
    }

    /**
     * 收藏视频
     */
    @PostMapping("/collect/{videoId}")
    @ApiOperation("收藏视频")
    public Result<?> collectVideo(@PathVariable Long videoId, @RequestParam(required = false) String folderName) {
        User user = getCurrentUser();
        videoCollectService.collectVideo(videoId, user.getId(), folderName);
        return Result.build();
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/collect/{videoId}")
    @ApiOperation("取消收藏")
    public Result<?> cancelCollect(@PathVariable Long videoId) {
        User user = getCurrentUser();
        videoCollectService.cancelCollect(videoId, user.getId());
        return Result.build();
    }

    /**
     * 查询用户收藏
     */
    @GetMapping("/collect/list")
    @ApiOperation("查询用户收藏")
    public Result<Page<VideoCollect>> getUserCollects(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User user = getCurrentUser();
        Page<VideoCollect> page = videoCollectService.getUserCollects(user.getId(), pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/collect/{videoId}")
    @ApiOperation("检查是否已收藏")
    public Result<Boolean> isCollected(@PathVariable Long videoId) {
        User user = getCurrentUser();
        boolean isCollected = videoCollectService.isCollected(videoId, user.getId());
        return Result.build(isCollected);
    }

    /**
     * 添加评论
     */
    @PostMapping("/comment")
    @ApiOperation("添加评论")
    public Result<VideoComment> addComment(@RequestBody VideoComment comment) {
        User user = getCurrentUser();
        VideoComment result = videoCommentService.addComment(comment, user);
        return Result.build(result);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    @ApiOperation("删除评论")
    public Result<?> deleteComment(@PathVariable Long commentId) {
        User user = getCurrentUser();
        videoCommentService.deleteComment(commentId, user);
        return Result.build();
    }

    /**
     * 分页查询视频评论
     */
    @GetMapping("/comment/{videoId}")
    @ApiOperation("分页查询视频评论")
    public Result<Page<VideoComment>> getVideoComments(
            @PathVariable Long videoId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<VideoComment> page = videoCommentService.getVideoComments(videoId, parentId, pageNum, pageSize);
        return Result.build(page);
    }
}
