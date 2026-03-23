package com.pilipili.entity.in;

import lombok.Data;

import java.util.List;

@Data
public class CreateTranscodeTaskRequest {
    private String targetType;
    private List<Long> targetIds;
    private String outputMode;
}
