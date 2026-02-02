package com.pilipili.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.Video;
import com.pilipili.mapper.VideoMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoRepository extends ServiceImpl<VideoMapper, Video> {
}
