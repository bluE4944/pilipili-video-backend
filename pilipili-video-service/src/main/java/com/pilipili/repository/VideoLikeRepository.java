package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoLike;
import com.pilipili.mapper.VideoLikeMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频点赞Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoLikeRepository extends ServiceImpl<VideoLikeMapper, VideoLike> {
}
