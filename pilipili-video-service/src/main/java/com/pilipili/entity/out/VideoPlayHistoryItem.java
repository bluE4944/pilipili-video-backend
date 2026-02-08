package com.pilipili.entity.out;

import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoPlayHistory;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("最近播放视频条目")
public class VideoPlayHistoryItem {

    @ApiModelProperty("播放记录")
    private VideoPlayHistory history;

    @ApiModelProperty("条目类型：collection 或 video")
    private String itemType;

    @ApiModelProperty("合集信息")
    private VideoCollection collection;

    @ApiModelProperty("视频信息（itemType=video）")
    private Video video;
}
