package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频分集实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_episode")
@ApiModel("视频分集实体")
public class VideoEpisode extends BaseEntity {

    /**
     * 合集ID
     */
    @ApiModelProperty("合集ID")
    private Long collectionId;

    /**
     * 视频ID（如果已上传到系统）
     */
    @ApiModelProperty("视频ID（如果已上传到系统）")
    private Long videoId;

    /**
     * 集数标识（如 "01", "02"）
     */
    @ApiModelProperty("集数标识（如 \"01\", \"02\"）")
    private String episodeNumber;

    /**
     * 分集名称
     */
    @ApiModelProperty("分集名称")
    private String episodeName;

    /**
     * 文件路径
     */
    @ApiModelProperty("文件路径")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    /**
     * 文件修改时间
     */
    @ApiModelProperty("文件修改时间")
    private java.util.Date fileModifyTime;

    /**
     * 文件格式
     */
    @ApiModelProperty("文件格式")
    private String fileFormat;

    /**
     * 排序序号
     */
    @ApiModelProperty("排序序号")
    private Integer sortOrder;
}
