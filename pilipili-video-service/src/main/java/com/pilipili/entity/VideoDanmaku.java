package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频弹幕实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_danmaku")
@ApiModel("视频弹幕实体")
public class VideoDanmaku extends BaseEntity {

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
     * 弹幕内容
     */
    @ApiModelProperty("弹幕内容")
    private String content;

    /**
     * 弹幕出现时间（秒）
     */
    @ApiModelProperty("弹幕出现时间（秒）")
    private Integer time;

    /**
     * 弹幕类型：1-滚动，2-顶部，3-底部
     */
    @ApiModelProperty("弹幕类型：1-滚动，2-顶部，3-底部")
    private Integer type;

    /**
     * 弹幕颜色
     */
    @ApiModelProperty("弹幕颜色")
    private String color;

    /**
     * 字体大小
     */
    @ApiModelProperty("字体大小")
    private Integer fontSize;
}
