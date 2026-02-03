package com.pilipili.entity.out;

import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 混合视频列表项（合集/单视频）
 */
@Data
@ApiModel("混合视频列表项")
public class VideoListItem {

    /**
     * 列表项类型：collection 或 video
     */
    @ApiModelProperty("列表项类型：collection 或 video")
    private String itemType;

    /**
     * 合集信息（itemType=collection 时返回）
     */
    @ApiModelProperty("合集信息")
    private VideoCollection collection;

    /**
     * 单视频信息（itemType=video 时返回）
     */
    @ApiModelProperty("单视频信息")
    private Video video;
}
