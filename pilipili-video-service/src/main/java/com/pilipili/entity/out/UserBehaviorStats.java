package com.pilipili.entity.out;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * User behavior statistics payload.
 * @author Liam
 * @version 1.0
 */
@Data
public class UserBehaviorStats {

    private Integer totalWatchTime;
    private Integer watchedVideoCount;
    private Long totalPlayCount;
    private Long playCount;
    private Long todayPlayCount;
    private Long weekPlayCount;
    private Long monthPlayCount;
    private Date lastPlayTime;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private Long favoriteCount;
    private List<String> recentWatchedVideos;
}
