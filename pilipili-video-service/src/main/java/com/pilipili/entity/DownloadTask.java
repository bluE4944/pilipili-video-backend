package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_download_task")
@ApiModel("下载任务")
public class DownloadTask extends BaseEntity {

    @ApiModelProperty("任务标签")
    private String taskTag;

    @ApiModelProperty("种子哈希")
    private String torrentHash;

    @ApiModelProperty("来源类型")
    private String sourceType;

    @ApiModelProperty("来源名称")
    private String sourceName;

    @ApiModelProperty("本地文件夹配置ID")
    private Long folderConfigId;

    @ApiModelProperty("保存目录")
    private String savePath;

    @ApiModelProperty("qB 分类")
    private String category;

    @ApiModelProperty("qB 原始状态")
    private String qbtState;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("进度百分比")
    private Double progress;

    @ApiModelProperty("已下载字节数")
    private Long downloadedBytes;

    @ApiModelProperty("总字节数")
    private Long totalBytes;

    @ApiModelProperty("下载速度")
    private Long downloadSpeed;

    @ApiModelProperty("预计剩余秒数")
    private Long etaSeconds;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("自动入库状态")
    private String autoImportStatus;

    @ApiModelProperty("自动入库消息")
    private String autoImportMessage;

    @ApiModelProperty("最后同步时间")
    private Date lastSyncedAt;
}
