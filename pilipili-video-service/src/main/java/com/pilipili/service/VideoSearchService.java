package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.Video;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 视频搜索服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoSearchService {

    private final VideoRepository videoRepository;

    /**
     * 关键词搜索
     */
    public Page<Video> searchByKeyword(String keyword, Integer pageNum, Integer pageSize) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.like("title", keyword)
                .or().like("description", keyword)
                .or().like("tags", keyword));
        wrapper.eq("status", 1); // 只搜索已上线的视频
        wrapper.orderByDesc("play_count", "create_time");
        return videoRepository.page(page, wrapper);
    }

    /**
     * 分类搜索
     */
    public Page<Video> searchByCategory(Long categoryId, Integer pageNum, Integer pageSize) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        wrapper.eq("status", 1);
        wrapper.orderByDesc("create_time");
        return videoRepository.page(page, wrapper);
    }

    /**
     * 标签搜索
     */
    public Page<Video> searchByTag(String tag, Integer pageNum, Integer pageSize) {
        Page<Video> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.like("tags", tag);
        wrapper.eq("status", 1);
        wrapper.orderByDesc("play_count", "create_time");
        return videoRepository.page(page, wrapper);
    }

    /**
     * 相关视频推荐
     */
    public List<Video> getRelatedVideos(Long videoId, Integer limit) {
        Video video = videoRepository.getById(videoId);
        if (video == null) {
            return Collections.emptyList();
        }

        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.ne("id", videoId);
        wrapper.eq("status", 1);

        // 优先推荐同分类的视频
        if (video.getCategoryId() != null) {
            wrapper.eq("category_id", video.getCategoryId());
        }

        // 如果有标签，也考虑标签相似度
        if (video.getTags() != null && !video.getTags().isEmpty()) {
            String[] tags = video.getTags().split(",");
            if (tags.length > 0) {
                wrapper.like("tags", tags[0]);
            }
        }

        wrapper.orderByDesc("play_count", "like_count");
        wrapper.last("LIMIT " + limit);
        return videoRepository.list(wrapper);
    }

    /**
     * 热门视频推荐
     */
    public List<Video> getHotVideos(Integer limit) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByDesc("play_count", "like_count", "create_time");
        wrapper.last("LIMIT " + limit);
        return videoRepository.list(wrapper);
    }
}
