package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoComment;
import com.pilipili.mapper.VideoCommentMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频评论Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoCommentRepository extends ServiceImpl<VideoCommentMapper, VideoComment> {
}
