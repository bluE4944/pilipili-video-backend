package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频评论实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_comment")
@ApiModel("视频评论实体")
public class VideoComment extends BaseEntity {

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
     * 用户名
     */
    @ApiModelProperty("用户名")
    private String userName;

    /**
     * 用户头像
     */
    @ApiModelProperty("用户头像")
    private String userAvatar;

    /**
     * 评论内容
     */
    @ApiModelProperty("评论内容")
    private String content;

    /**
     * 父评论ID（0表示顶级评论）
     */
    @ApiModelProperty("父评论ID（0表示顶级评论）")
    private Long parentId;

    /**
     * 点赞数
     */
    @ApiModelProperty("点赞数")
    private Long likeCount;

    /**
     * 回复数
     */
    @ApiModelProperty("回复数")
    private Long replyCount;
}
