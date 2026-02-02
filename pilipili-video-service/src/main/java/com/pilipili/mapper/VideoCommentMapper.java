package com.pilipili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pilipili.entity.VideoComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频评论Mapper
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Mapper
public interface VideoCommentMapper extends BaseMapper<VideoComment> {
}
