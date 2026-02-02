package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.mapper.VideoEpisodeMapper;
import org.springframework.stereotype.Repository;

/**
 * 视频分集Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class VideoEpisodeRepository extends ServiceImpl<VideoEpisodeMapper, VideoEpisode> {
}
