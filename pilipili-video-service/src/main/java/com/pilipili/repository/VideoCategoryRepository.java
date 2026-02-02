package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoCategory;
import com.pilipili.mapper.VideoCategoryMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频分类Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoCategoryRepository extends ServiceImpl<VideoCategoryMapper, VideoCategory> {
}
