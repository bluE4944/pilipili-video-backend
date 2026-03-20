package com.pilipili.entity.out;

/**
 * 视频播放源信息
 */
public class VideoPlaySourceInfo {

    private String playUrl;
    private String sourceMode;
    private String processMode;
    private Boolean browserFallbackAllowed;

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public void setSourceMode(String sourceMode) {
        this.sourceMode = sourceMode;
    }

    public String getProcessMode() {
        return processMode;
    }

    public void setProcessMode(String processMode) {
        this.processMode = processMode;
    }

    public Boolean getBrowserFallbackAllowed() {
        return browserFallbackAllowed;
    }

    public void setBrowserFallbackAllowed(Boolean browserFallbackAllowed) {
        this.browserFallbackAllowed = browserFallbackAllowed;
    }
}
