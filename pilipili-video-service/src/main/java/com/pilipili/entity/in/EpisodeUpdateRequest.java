package com.pilipili.entity.in;

import lombok.Data;

/**
 * 分集批量调整请求项
 */
@Data
public class EpisodeUpdateRequest {
    private Long id;
    private String episodeNumber;
    private String episodeName;
    private Integer sortOrder;
}
