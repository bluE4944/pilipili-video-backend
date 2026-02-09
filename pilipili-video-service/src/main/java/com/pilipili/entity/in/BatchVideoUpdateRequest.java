package com.pilipili.entity.in;

import lombok.Data;

import java.util.List;

/**
 * 批量更新视频请求
 */
@Data
public class BatchVideoUpdateRequest {
    private List<Long> videoIds;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String tags;
}
