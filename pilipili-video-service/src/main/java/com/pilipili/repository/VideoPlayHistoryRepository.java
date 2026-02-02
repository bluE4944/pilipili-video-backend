package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoPlayHistory;
import com.pilipili.mapper.VideoPlayHistoryMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频播放历史Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoPlayHistoryRepository extends ServiceImpl<VideoPlayHistoryMapper, VideoPlayHistory> {
}
