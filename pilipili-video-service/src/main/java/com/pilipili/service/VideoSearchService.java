package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.out.VideoListItem;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

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
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoCollectionRepository videoCollectionRepository;

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
        Page<Video> result = videoRepository.page(page, wrapper);
        normalizePageCoverUrl(result);
        return result;
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
        Page<Video> result = videoRepository.page(page, wrapper);
        normalizePageCoverUrl(result);
        return result;
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
        Page<Video> result = videoRepository.page(page, wrapper);
        normalizePageCoverUrl(result);
        return result;
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
        List<Video> videos = videoRepository.list(wrapper);
        normalizeListCoverUrl(videos);
        return videos;
    }

    /**
     * 热门视频推荐
     */
    public List<VideoListItem> getHotVideos(Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        int pageSize = Math.min(Math.max(size * 5, 50), 200);
        int maxScan = 1000;
        int pageNum = 1;
        List<Video> collected = new ArrayList<>();
        while (collected.size() < maxScan) {
            Page<Video> page = new Page<>(pageNum, pageSize);
            QueryWrapper<Video> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1);
            wrapper.orderByDesc("play_count", "like_count", "create_time");
            Page<Video> result = videoRepository.page(page, wrapper);
            List<Video> records = result.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            collected.addAll(records);
            List<VideoListItem> items = buildMixedItemsFromVideos(collected, size);
            if (items.size() >= size) {
                return items;
            }
            if (records.size() < pageSize) {
                break;
            }
            pageNum += 1;
        }
        if (collected.isEmpty()) {
            return Collections.emptyList();
        }
        return buildMixedItemsFromVideos(collected, size);
    }

    private List<VideoListItem> buildMixedItemsFromVideos(List<Video> videos, int limit) {
        List<Long> videoIds = videos.stream()
                .map(Video::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(new QueryWrapper<VideoEpisode>().in("video_id", videoIds));
        Map<Long, Long> videoToCollection = episodes.stream()
                .filter(e -> e.getVideoId() != null && e.getCollectionId() != null)
                .collect(Collectors.toMap(VideoEpisode::getVideoId, VideoEpisode::getCollectionId, (a, b) -> a));

        Set<Long> collectionIds = new HashSet<>(videoToCollection.values());
        Map<Long, VideoCollection> collectionMap = collectionIds.isEmpty()
                ? Collections.emptyMap()
                : videoCollectionRepository.listByIds(collectionIds).stream()
                .collect(Collectors.toMap(VideoCollection::getId, c -> c, (a, b) -> a));
        fillCollectionPlayCounts(new ArrayList<>(collectionMap.values()));

        List<VideoListItem> items = new ArrayList<>();
        Set<Long> seenCollections = new HashSet<>();
        Set<Long> seenVideos = new HashSet<>();
        for (Video video : videos) {
            if (items.size() >= limit) {
                break;
            }
            if (video == null || video.getId() == null) {
                continue;
            }
            Long collectionId = videoToCollection.get(video.getId());
            if (collectionId != null) {
                if (!seenCollections.add(collectionId)) {
                    continue;
                }
                VideoCollection collection = collectionMap.get(collectionId);
                if (collection == null) {
                    continue;
                }
                normalizeCollectionCoverUrl(collection);
                VideoListItem item = new VideoListItem();
                item.setItemType("collection");
                item.setCollection(collection);
                items.add(item);
                continue;
            }

            if (!seenVideos.add(video.getId())) {
                continue;
            }
            normalizeVideoCoverUrl(video);
            VideoListItem item = new VideoListItem();
            item.setItemType("video");
            item.setVideo(video);
            items.add(item);
        }
        return items;
    }

    private void fillCollectionPlayCounts(List<VideoCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return;
        }
        List<Long> collectionIds = collections.stream()
                .map(VideoCollection::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (collectionIds.isEmpty()) {
            return;
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(
                new QueryWrapper<VideoEpisode>().in("collection_id", collectionIds)
        );
        if (episodes == null || episodes.isEmpty()) {
            for (VideoCollection collection : collections) {
                collection.setPlayCount(0L);
                collection.setLikeCount(0L);
                collection.setCollectCount(0L);
            }
            return;
        }

        Set<Long> videoIds = episodes.stream()
                .map(VideoEpisode::getVideoId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Video> videoMap = videoIds.isEmpty()
                ? Collections.emptyMap()
                : videoRepository.listByIds(videoIds).stream()
                .filter(video -> video.getId() != null)
                .collect(Collectors.toMap(Video::getId, video -> video, (a, b) -> a));

        Map<Long, Long> collectionPlayMap = new HashMap<>();
        Map<Long, Long> collectionLikeMap = new HashMap<>();
        Map<Long, Long> collectionCollectMap = new HashMap<>();
        for (VideoEpisode episode : episodes) {
            Long collectionId = episode.getCollectionId();
            Long videoId = episode.getVideoId();
            if (collectionId == null || videoId == null) {
                continue;
            }
            Video video = videoMap.get(videoId);
            Long playCount = video != null && video.getPlayCount() != null ? video.getPlayCount() : 0L;
            Long likeCount = video != null && video.getLikeCount() != null ? video.getLikeCount() : 0L;
            Long collectCount = video != null && video.getCollectCount() != null ? video.getCollectCount() : 0L;
            collectionPlayMap.merge(collectionId, playCount, Long::sum);
            collectionLikeMap.merge(collectionId, likeCount, Long::sum);
            collectionCollectMap.merge(collectionId, collectCount, Long::sum);
        }

        for (VideoCollection collection : collections) {
            Long id = collection.getId();
            if (id == null) {
                continue;
            }
            collection.setPlayCount(collectionPlayMap.getOrDefault(id, 0L));
            collection.setLikeCount(collectionLikeMap.getOrDefault(id, 0L));
            collection.setCollectCount(collectionCollectMap.getOrDefault(id, 0L));
        }
    }
    private void normalizePageCoverUrl(Page<Video> page) {
        if (page == null || page.getRecords() == null) {
            return;
        }
        for (Video video : page.getRecords()) {
            normalizeVideoCoverUrl(video);
        }
    }

    private void normalizeListCoverUrl(List<Video> videos) {
        if (videos == null) {
            return;
        }
        for (Video video : videos) {
            normalizeVideoCoverUrl(video);
        }
    }

    private void normalizeVideoCoverUrl(Video video) {
        if (video == null || video.getId() == null) {
            return;
        }
        String coverUrl = video.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            video.setCoverUrl("/api/cover/video/" + video.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        video.setCoverUrl("/api/cover/video/" + video.getId());
    }

    private void normalizeCollectionCoverUrl(VideoCollection collection) {
        if (collection == null || collection.getId() == null) {
            return;
        }
        String coverUrl = collection.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            collection.setCoverUrl("/api/cover/collection/" + collection.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        collection.setCoverUrl("/api/cover/collection/" + collection.getId());
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
