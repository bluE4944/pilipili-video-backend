package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoComment;
import com.pilipili.repository.VideoCommentRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 视频评论服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoCommentService {

    private final VideoCommentRepository videoCommentRepository;
    private final VideoRepository videoRepository;

    /**
     * 添加评论
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoComment addComment(VideoComment comment, User user) {
        Video video = videoRepository.getById(comment.getVideoId());
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        comment.setUserId(user.getId());
        comment.setUserName(user.getUsername());
        comment.setUserAvatar(user.getNikeName()); // 简化处理
        comment.setLikeCount(0L);
        comment.setReplyCount(0L);
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        comment.setCreateTime(new Date());
        comment.setLogicDel(0);
        videoCommentRepository.save(comment);

        // 更新视频评论数
        QueryWrapper<VideoComment> countWrapper = new QueryWrapper<>();
        countWrapper.eq("video_id", comment.getVideoId());
        countWrapper.eq("parent_id", 0);
        long commentCount = videoCommentRepository.count(countWrapper);
        video.setCommentCount(commentCount);
        videoRepository.updateById(video);

        // 如果是回复，更新父评论的回复数
        if (comment.getParentId() > 0) {
            VideoComment parentComment = videoCommentRepository.getById(comment.getParentId());
            if (parentComment != null) {
                QueryWrapper<VideoComment> replyWrapper = new QueryWrapper<>();
                replyWrapper.eq("parent_id", comment.getParentId());
                long replyCount = videoCommentRepository.count(replyWrapper);
                parentComment.setReplyCount(replyCount);
                videoCommentRepository.updateById(parentComment);
            }
        }

        log.info("评论添加成功: commentId={}, videoId={}, userId={}", comment.getId(), comment.getVideoId(), user.getId());
        return comment;
    }

    /**
     * 删除评论
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, User user) {
        VideoComment comment = videoCommentRepository.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 只有评论者或管理员可以删除
        if (!comment.getUserId().equals(user.getId()) && !"admin".equals(user.getRole())) {
            throw new RuntimeException("无权限删除此评论");
        }

        videoCommentRepository.removeById(commentId);

        // 更新视频评论数
        Video video = videoRepository.getById(comment.getVideoId());
        if (video != null) {
            QueryWrapper<VideoComment> countWrapper = new QueryWrapper<>();
            countWrapper.eq("video_id", comment.getVideoId());
            countWrapper.eq("parent_id", 0);
            long commentCount = videoCommentRepository.count(countWrapper);
            video.setCommentCount(commentCount);
            videoRepository.updateById(video);
        }

        log.info("评论删除成功: commentId={}", commentId);
    }

    /**
     * 分页查询视频评论
     */
    public Page<VideoComment> getVideoComments(Long videoId, Long parentId, Integer pageNum, Integer pageSize) {
        Page<VideoComment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<VideoComment> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        if (parentId != null) {
            wrapper.eq("parent_id", parentId);
        } else {
            wrapper.eq("parent_id", 0);
        }
        wrapper.orderByDesc("create_time");
        return videoCommentRepository.page(page, wrapper);
    }
}
