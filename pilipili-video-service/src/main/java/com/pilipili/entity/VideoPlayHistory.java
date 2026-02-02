package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频播放历史实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_play_history")
@ApiModel("视频播放历史实体")
public class VideoPlayHistory extends BaseEntity {

    /**
     * 视频ID
     */
    @ApiModelProperty("视频ID")
    private Long videoId;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 播放进度（秒）
     */
    @ApiModelProperty("播放进度（秒）")
    private Integer progress;

    /**
     * 播放时长（秒）
     */
    @ApiModelProperty("播放时长（秒）")
    private Integer playDuration;

    /**
     * 播放清晰度：1-标清，2-高清，3-超清
     */
    @ApiModelProperty("播放清晰度：1-标清，2-高清，3-超清")
    private Integer quality;

    /**
     * 播放倍速
     */
    @ApiModelProperty("播放倍速")
    private Double playbackRate;
}
