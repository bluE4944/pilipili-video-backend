package com.pilipili.entity.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TranscodeTaskVO {
    private Long taskId;
    private String targetType;
    private String outputMode;
    private String status;
    private Integer totalFileCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private String currentFile;
    private Double currentFileProgress;
    private Double totalProgress;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private List<TranscodeTaskItemVO> items;
}
