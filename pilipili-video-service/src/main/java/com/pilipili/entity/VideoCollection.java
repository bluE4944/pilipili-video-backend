package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频合集实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_collection")
@ApiModel("视频合集实体")
public class VideoCollection extends BaseEntity {

    /**
     * 合集标题
     */
    @ApiModelProperty("合集标题")
    private String title;

    /**
     * 合集描述
     */
    @ApiModelProperty("合集描述")
    private String description;

    /**
     * 合集封面URL
     */
    @ApiModelProperty("合集封面URL")
    private String coverUrl;

    /**
     * 源文件夹路径
     */
    @ApiModelProperty("源文件夹路径")
    private String sourceFolderPath;

    /**
     * 视频数量
     */
    @ApiModelProperty("视频数量")
    private Integer videoCount;

    /**
     * 合集类型：1-自动整合，2-手动创建
     */
    @ApiModelProperty("合集类型：1-自动整合，2-手动创建")
    private Integer collectionType;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;
}
