package com.pilipili.entity.in;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 视频查询条件
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@ApiModel("视频查询条件")
public class VideoCondition {
    /**
     * 视频标题
     */
    @ApiModelProperty("视频标题")
    private String title;

    /**
     * 分类ID
     */
    @ApiModelProperty("分类ID")
    private Long categoryId;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 视频状态
     */
    @ApiModelProperty("视频状态")
    private Integer status;

    /**
     * 标签
     */
    @ApiModelProperty("标签")
    private String tags;
}
