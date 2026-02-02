package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.VideoDanmaku;
import com.pilipili.repository.VideoDanmakuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 视频弹幕服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoDanmakuService {

    private final VideoDanmakuRepository videoDanmakuRepository;

    /**
     * 添加弹幕
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoDanmaku addDanmaku(VideoDanmaku danmaku) {
        danmaku.setCreateTime(new Date());
        danmaku.setLogicDel(0);
        if (danmaku.getType() == null) {
            danmaku.setType(1); // 默认滚动
        }
        if (danmaku.getColor() == null) {
            danmaku.setColor("#FFFFFF"); // 默认白色
        }
        if (danmaku.getFontSize() == null) {
            danmaku.setFontSize(25); // 默认字体大小
        }
        videoDanmakuRepository.save(danmaku);
        log.info("弹幕添加成功: danmakuId={}, videoId={}", danmaku.getId(), danmaku.getVideoId());
        return danmaku;
    }

    /**
     * 获取视频弹幕列表
     */
    public List<VideoDanmaku> getVideoDanmakus(Long videoId) {
        QueryWrapper<VideoDanmaku> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        wrapper.orderByAsc("time");
        return videoDanmakuRepository.list(wrapper);
    }

    /**
     * 删除弹幕
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDanmaku(Long danmakuId) {
        videoDanmakuRepository.removeById(danmakuId);
        log.info("弹幕删除成功: danmakuId={}", danmakuId);
    }
}
