package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频分类实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_category")
@ApiModel("视频分类实体")
public class VideoCategory extends BaseEntity {

    /**
     * 分类名称
     */
    @ApiModelProperty("分类名称")
    private String categoryName;

    /**
     * 分类描述
     */
    @ApiModelProperty("分类描述")
    private String description;

    /**
     * 父分类ID（0表示顶级分类）
     */
    @ApiModelProperty("父分类ID（0表示顶级分类）")
    private Long parentId;

    /**
     * 排序序号
     */
    @ApiModelProperty("排序序号")
    private Integer sortOrder;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;
}
