package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频收藏实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_video_collect")
@ApiModel("视频收藏实体")
public class VideoCollect extends BaseEntity {

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
     * 收藏夹名称
     */
    @ApiModelProperty("收藏夹名称")
    private String folderName;
}
