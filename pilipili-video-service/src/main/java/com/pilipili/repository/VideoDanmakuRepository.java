package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoDanmaku;
import com.pilipili.mapper.VideoDanmakuMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频弹幕Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoDanmakuRepository extends ServiceImpl<VideoDanmakuMapper, VideoDanmaku> {
}
