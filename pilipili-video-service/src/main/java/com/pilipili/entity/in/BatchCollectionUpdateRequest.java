package com.pilipili.entity.in;

import lombok.Data;

import java.util.List;

/**
 * 批量更新合集请求
 */
@Data
public class BatchCollectionUpdateRequest {
    private List<Long> collectionIds;
    private String title;
    private String description;
}
