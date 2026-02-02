package com.pilipili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pilipili.entity.VideoLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频点赞Mapper
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Mapper
public interface VideoLikeMapper extends BaseMapper<VideoLike> {
}
