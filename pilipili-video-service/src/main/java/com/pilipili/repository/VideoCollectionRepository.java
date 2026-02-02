package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoCollection;
import com.pilipili.mapper.VideoCollectionMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频合集Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoCollectionRepository extends ServiceImpl<VideoCollectionMapper, VideoCollection> {
}
