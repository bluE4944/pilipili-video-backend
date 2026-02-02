package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频点赞实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_like")
@ApiModel("视频点赞实体")
public class VideoLike extends BaseEntity {

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
     * 是否点赞：0-取消点赞，1-点赞
     */
    @ApiModelProperty("是否点赞：0-取消点赞，1-点赞")
    private Integer isLike;
}
