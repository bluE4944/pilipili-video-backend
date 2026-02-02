package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video")
@ApiModel("视频实体")
public class Video extends BaseEntity {

    /**
     * 视频标题
     */
    @ApiModelProperty("视频标题")
    private String title;

    /**
     * 视频描述
     */
    @ApiModelProperty("视频描述")
    private String description;

    /**
     * 视频封面URL
     */
    @ApiModelProperty("视频封面URL")
    private String coverUrl;

    /**
     * 视频文件URL
     */
    @ApiModelProperty("视频文件URL")
    private String videoUrl;

    /**
     * 视频时长（秒）
     */
    @ApiModelProperty("视频时长（秒）")
    private Integer duration;

    /**
     * 视频大小（字节）
     */
    @ApiModelProperty("视频大小（字节）")
    private Long fileSize;

    /**
     * 视频格式
     */
    @ApiModelProperty("视频格式")
    private String format;

    /**
     * 视频分类ID
     */
    @ApiModelProperty("视频分类ID")
    private Long categoryId;

    /**
     * 视频分类名称
     */
    @ApiModelProperty("视频分类名称")
    private String categoryName;

    /**
     * 视频标签（逗号分隔）
     */
    @ApiModelProperty("视频标签（逗号分隔）")
    private String tags;

    /**
     * 上传用户ID
     */
    @ApiModelProperty("上传用户ID")
    private Long userId;

    /**
     * 上传用户名
     */
    @ApiModelProperty("上传用户名")
    private String userName;

    /**
     * 视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过
     */
    @ApiModelProperty("视频状态：0-待审核，1-已上线，2-已下架，3-审核不通过")
    private Integer status;

    /**
     * 播放量
     */
    @ApiModelProperty("播放量")
    private Long playCount;

    /**
     * 点赞数
     */
    @ApiModelProperty("点赞数")
    private Long likeCount;

    /**
     * 收藏数
     */
    @ApiModelProperty("收藏数")
    private Long collectCount;

    /**
     * 评论数
     */
    @ApiModelProperty("评论数")
    private Long commentCount;

    /**
     * 视频清晰度：1-标清，2-高清，3-超清
     */
    @ApiModelProperty("视频清晰度：1-标清，2-高清，3-超清")
    private Integer quality;

    /**
     * 审核意见
     */
    @ApiModelProperty("审核意见")
    private String auditRemark;
}
