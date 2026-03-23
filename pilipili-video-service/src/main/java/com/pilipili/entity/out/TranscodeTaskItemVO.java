package com.pilipili.entity.out;

import lombok.Data;

@Data
public class TranscodeTaskItemVO {
    private Integer index;
    private String sourcePath;
    private String outputPath;
    private String status;
    private Double progress;
    private String message;
}
