package com.pilipili.entity.in;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("创建磁力下载任务请求")
public class CreateDownloadMagnetTaskRequest {

    @ApiModelProperty("磁力链接")
    private String magnetUrl;

    @ApiModelProperty("文件夹配置ID")
    private Long folderConfigId;

    @ApiModelProperty("添加后暂停")
    private Boolean addPaused;
}
