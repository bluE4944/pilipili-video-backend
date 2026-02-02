package com.pilipili.controller;

import com.pilipili.entity.VideoDanmaku;
import com.pilipili.service.VideoDanmakuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 弹幕WebSocket控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Controller
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DanmakuWebSocketController {

    private final VideoDanmakuService videoDanmakuService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 接收弹幕消息并广播
     */
    @MessageMapping("/danmaku/send")
    @SendTo("/topic/danmaku/{videoId}")
    public VideoDanmaku sendDanmaku(VideoDanmaku danmaku) {
        // 保存弹幕
        VideoDanmaku saved = videoDanmakuService.addDanmaku(danmaku);
        log.info("弹幕广播: videoId={}, danmakuId={}", danmaku.getVideoId(), saved.getId());
        return saved;
    }

    /**
     * 向指定视频房间广播弹幕
     */
    public void broadcastDanmaku(Long videoId, VideoDanmaku danmaku) {
        messagingTemplate.convertAndSend("/topic/danmaku/" + videoId, danmaku);
    }
}
