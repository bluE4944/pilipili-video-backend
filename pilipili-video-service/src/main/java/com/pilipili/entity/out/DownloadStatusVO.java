package com.pilipili.entity.out;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("下载器状态")
public class DownloadStatusVO {

    @ApiModelProperty("是否连通")
    private Boolean connected;

    @ApiModelProperty("状态消息")
    private String message;

    @ApiModelProperty("qB 版本")
    private String version;

    @ApiModelProperty("默认分类")
    private String defaultCategory;

    @ApiModelProperty("默认保存目录")
    private String defaultSavePath;

    @ApiModelProperty("轮询间隔秒数")
    private Integer pollIntervalSeconds;

    @ApiModelProperty("最后探测时间")
    private Date lastCheckedAt;
}
