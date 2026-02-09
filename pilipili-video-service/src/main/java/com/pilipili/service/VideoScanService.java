package com.pilipili.service;

import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.repository.VideoRepository;
import com.pilipili.utils.VideoCoverUtil;
import com.pilipili.utils.VideoFileScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 视频扫描服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoScanService {

    private static final String DEFAULT_COVER_FOLDER = "covers";

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${video.cover.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${video.scan.collection.episode-hit-rate:0.7}")
    private double episodeHitRate;

    @Value("${video.scan.collection.title-match-rate:0.7}")
    private double titleMatchRate;

    private final VideoFileScanner videoFileScanner;
    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoRepository videoRepository;
    private final LocalFolderConfigService localFolderConfigService;

    private static class ScanCount {
        private final int collectionCount;
        private final int singleCount;

        private ScanCount(int collectionCount, int singleCount) {
            this.collectionCount = collectionCount;
            this.singleCount = singleCount;
        }

        private int getCollectionCount() {
            return collectionCount;
        }

        private int getSingleCount() {
            return singleCount;
        }
    }

    /**
     * 扫描文件夹并创建合集
     */
    @Transactional(rollbackFor = Exception.class)
    public void scanAndCreateCollections(Long configId, User user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        LocalFolderConfig config = localFolderConfigService.getConfigById(configId);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }

        // 更新扫描状态
        localFolderConfigService.updateScanStatus(configId, 1, new Date());

        try {
            // 扫描视频文件
            List<VideoFileScanner.VideoFileInfo> videoFiles = videoFileScanner.scanFolder(config.getFolderPath());
            log.info("扫描到 {} 个视频文件", videoFiles.size());

            Map<String, List<VideoFileScanner.VideoFileInfo>> folderGroups = groupByParentFolder(videoFiles);
            int collectionCount = 0;
            int singleCount = 0;
            for (Map.Entry<String, List<VideoFileScanner.VideoFileInfo>> entry : folderGroups.entrySet()) {
                String folderPath = entry.getKey();
                String folderName = getFolderName(folderPath);
                List<VideoFileScanner.VideoFileInfo> files = entry.getValue();

                Map<String, List<VideoFileScanner.VideoFileInfo>> tagGroups = videoFileScanner.groupBySeparateTags(files);
                if (tagGroups.size() > 1) {
                    for (Map.Entry<String, List<VideoFileScanner.VideoFileInfo>> tagEntry : tagGroups.entrySet()) {
                        String tagKey = tagEntry.getKey();
                        String taggedFolderName = applyTagSuffix(folderName, tagKey);
                        ScanCount count = processFolderGroup(tagEntry.getValue(), taggedFolderName, folderPath, user);
                        collectionCount += count.getCollectionCount();
                        singleCount += count.getSingleCount();
                    }
                    continue;
                }

                ScanCount count = processFolderGroup(files, folderName, folderPath, user);
                collectionCount += count.getCollectionCount();
                singleCount += count.getSingleCount();
            }
            log.info("扫描分组完成: collectionCount={}, singleVideoCount={}", collectionCount, singleCount);

            // 更新扫描状态为完成
            localFolderConfigService.updateScanStatus(configId, 2, new Date());
            log.info("扫描完成: configId={}", configId);

        } catch (Exception e) {
            log.error("扫描失败: configId={}", configId, e);
            localFolderConfigService.updateScanStatus(configId, 3, new Date());
            throw e;
        }
    }

    /**
     * 创建合集
     */
    private void createCollection(String title, List<VideoFileScanner.VideoFileInfo> files, String sourceFolder, User user) {
        VideoCollection existing = findExistingCollection(title, sourceFolder);
        VideoCollection collection;

        if (existing != null) {
            collection = existing;
        } else {
            collection = new VideoCollection();
            collection.setTitle(title);
            collection.setDescription("自动整合的视频合集");
            collection.setSourceFolderPath(sourceFolder);
            collection.setCollectionType(1);
            collection.setEnabled(1);
            collection.setCreateId(user.getId());
            collection.setCreateName(user.getUsername());
            collection.setCreateTime(new Date());
            collection.setLogicDel(0);
            videoCollectionRepository.save(collection);
        }

        files.sort((f1, f2) -> {
            String ep1 = f1.getEpisodeNumber() != null ? f1.getEpisodeNumber() : "999";
            String ep2 = f2.getEpisodeNumber() != null ? f2.getEpisodeNumber() : "999";
            return ep1.compareTo(ep2);
        });

        if (!files.isEmpty()) {
            applyCollectionCoverIfNeeded(collection, files.get(0), user);
        }

        QueryWrapper<VideoEpisode> episodeWrapper = new QueryWrapper<>();
        episodeWrapper.eq("collection_id", collection.getId());
        List<VideoEpisode> existingEpisodes = videoEpisodeRepository.list(episodeWrapper);
        Map<String, VideoEpisode> existingByPath = existingEpisodes.stream()
                .filter(e -> e.getFilePath() != null)
                .collect(Collectors.toMap(VideoEpisode::getFilePath, e -> e, (a, b) -> a));

        int sortOrder = existingEpisodes.size() + 1;
        int newCount = 0;
        for (VideoFileScanner.VideoFileInfo file : files) {
            VideoEpisode existingEpisode = existingByPath.get(file.getFilePath());
            if (existingEpisode != null) {
                if (existingEpisode.getVideoId() == null) {
                    Video video = getOrCreateVideo(file, user);
                    existingEpisode.setVideoId(video.getId());
                    existingEpisode.setUpdateTime(new Date());
                    videoEpisodeRepository.updateById(existingEpisode);
                } else {
                    ensureDefaultCover(existingEpisode.getVideoId(), file, user);
                }
                continue;
            }

            Video video = getOrCreateVideo(file, user);
            VideoEpisode episode = new VideoEpisode();
            episode.setCollectionId(collection.getId());
            episode.setVideoId(video.getId());
            episode.setEpisodeNumber(file.getEpisodeNumber());
            episode.setEpisodeName(file.getEpisodeNumber() != null
                    ? "第" + file.getEpisodeNumber() + "集" : file.getTitle());
            episode.setFilePath(file.getFilePath());
            episode.setFileSize(file.getFileSize());
            episode.setFileModifyTime(file.getFileModifyTime());
            episode.setFileFormat(file.getFileFormat());
            episode.setSortOrder(sortOrder++);
            episode.setCreateId(user.getId());
            episode.setCreateName(user.getUsername());
            episode.setCreateTime(new Date());
            episode.setLogicDel(0);
            videoEpisodeRepository.save(episode);
            newCount++;
        }

        collection.setVideoCount(existingEpisodes.size() + newCount);
        collection.setUpdateId(user.getId());
        collection.setUpdateName(user.getUsername());
        collection.setUpdateTime(new Date());
        videoCollectionRepository.updateById(collection);

        log.info("创建合集成功: collectionId={}, title={}, episodeCount={}",
                collection.getId(), title, collection.getVideoCount());
    }

    /**
     * 创建单个视频合集
     */
    private void createSingleVideoCollection(VideoFileScanner.VideoFileInfo file, String sourceFolder, User user) {
        VideoCollection existing = findExistingCollection(file.getTitle(), sourceFolder);
        VideoCollection collection;
        if (existing != null) {
            collection = existing;
        } else {
            collection = new VideoCollection();
            collection.setTitle(file.getTitle());
            collection.setDescription("单个视频文件");
            collection.setSourceFolderPath(sourceFolder);
            collection.setCollectionType(1);
            collection.setVideoCount(1);
            collection.setEnabled(1);
            collection.setCreateId(user.getId());
            collection.setCreateName(user.getUsername());
            collection.setCreateTime(new Date());
            collection.setLogicDel(0);
            videoCollectionRepository.save(collection);
        }

        QueryWrapper<VideoEpisode> episodeWrapper = new QueryWrapper<>();
        episodeWrapper.eq("collection_id", collection.getId());
        episodeWrapper.eq("file_path", file.getFilePath());
        VideoEpisode existingEpisode = videoEpisodeRepository.getOne(episodeWrapper);
        if (existingEpisode != null) {
            boolean coverUpdated = applyCollectionCoverIfNeeded(collection, file, user);
            if (existingEpisode.getVideoId() == null) {
                Video video = getOrCreateVideo(file, user);
                existingEpisode.setVideoId(video.getId());
                existingEpisode.setUpdateTime(new Date());
                videoEpisodeRepository.updateById(existingEpisode);
            } else {
                ensureDefaultCover(existingEpisode.getVideoId(), file, user);
            }
            if (coverUpdated) {
                videoCollectionRepository.updateById(collection);
            }
            return;
        }

        applyCollectionCoverIfNeeded(collection, file, user);
        Video video = getOrCreateVideo(file, user);
        VideoEpisode episode = new VideoEpisode();
        episode.setCollectionId(collection.getId());
        episode.setVideoId(video.getId());
        episode.setEpisodeName(file.getTitle());
        episode.setFilePath(file.getFilePath());
        episode.setFileSize(file.getFileSize());
        episode.setFileModifyTime(file.getFileModifyTime());
        episode.setFileFormat(file.getFileFormat());
        episode.setSortOrder(1);
        episode.setCreateId(user.getId());
        episode.setCreateName(user.getUsername());
        episode.setCreateTime(new Date());
        episode.setLogicDel(0);
        videoEpisodeRepository.save(episode);

        QueryWrapper<VideoEpisode> countWrapper = new QueryWrapper<>();
        countWrapper.eq("collection_id", collection.getId());
        long total = videoEpisodeRepository.count(countWrapper);
        collection.setVideoCount((int) total);
        collection.setUpdateId(user.getId());
        collection.setUpdateName(user.getUsername());
        collection.setUpdateTime(new Date());
        videoCollectionRepository.updateById(collection);
    }

    /**
     * 创建单个视频（不整合成合集）
     */
    private void createStandaloneVideo(VideoFileScanner.VideoFileInfo file, User user) {
        getOrCreateVideo(file, user);
    }

    /**
     * 查找已存在的合集
     */
    private VideoCollection findExistingCollection(String title, String sourceFolder) {
        QueryWrapper<VideoCollection> wrapper = new QueryWrapper<>();
        wrapper.eq("title", title);
        wrapper.eq("source_folder_path", sourceFolder);
        return videoCollectionRepository.getOne(wrapper);
    }

    private ScanCount processFolderGroup(List<VideoFileScanner.VideoFileInfo> files, String folderName, String folderPath, User user) {
        int collectionCount = 0;
        int singleCount = 0;
        if (isCollectionFolder(files, folderName)) {
            String collectionTitle = resolveCollectionTitle(files, folderName);
            createCollection(collectionTitle, files, folderPath, user);
            collectionCount++;
            return new ScanCount(collectionCount, singleCount);
        }

        Map<String, List<VideoFileScanner.VideoFileInfo>> groups = videoFileScanner.groupSimilarFiles(files);
        for (Map.Entry<String, List<VideoFileScanner.VideoFileInfo>> groupEntry : groups.entrySet()) {
            String groupTitle = resolveGroupTitle(groupEntry.getValue(), groupEntry.getKey());
            createCollection(groupTitle, groupEntry.getValue(), folderPath, user);
            collectionCount++;
        }

        List<VideoFileScanner.VideoFileInfo> singleFiles = files.stream()
                .filter(file -> groups.values().stream()
                        .noneMatch(group -> group.contains(file)))
                .collect(Collectors.toList());
        for (VideoFileScanner.VideoFileInfo file : singleFiles) {
            createStandaloneVideo(file, user);
            singleCount++;
        }
        return new ScanCount(collectionCount, singleCount);
    }

    private String applyTagSuffix(String baseName, String tagKey) {
        if (tagKey == null || tagKey.trim().isEmpty()) {
            return baseName;
        }
        String suffix = formatTagSuffix(tagKey);
        if (baseName == null || baseName.trim().isEmpty()) {
            return suffix;
        }
        return baseName.trim() + " " + suffix;
    }

    private String formatTagSuffix(String tagKey) {
        if (tagKey == null || tagKey.trim().isEmpty()) {
            return "";
        }
        String trimmed = tagKey.trim();
        if (trimmed.matches("[a-z0-9\\+]+")) {
            return trimmed.toUpperCase();
        }
        return trimmed;
    }

    private Video getOrCreateVideo(VideoFileScanner.VideoFileInfo file, User user) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("video_url", file.getFilePath());
        Video existing = videoRepository.getOne(wrapper);
        if (existing != null) {
            ensureDefaultCover(existing, file, user);
            return existing;
        }

        Video video = new Video();
        video.setTitle(buildVideoTitle(file));
        video.setDescription("本地扫描视频");
        String coverUrl = generateDefaultCover(file);
        if (coverUrl != null) {
            video.setCoverUrl(coverUrl);
        }
        video.setVideoUrl(file.getFilePath());
        video.setFileSize(file.getFileSize());
        video.setFormat(file.getFileFormat());
        video.setUserId(user.getId());
        video.setUserName(user.getUsername());
        video.setStatus(1);
        video.setPlayCount(0L);
        video.setLikeCount(0L);
        video.setCollectCount(0L);
        video.setCommentCount(0L);
        video.setQuality(1);
        video.setCreateId(user.getId());
        video.setCreateName(user.getUsername());
        video.setCreateTime(new Date());
        video.setLogicDel(0);
        videoRepository.save(video);
        return video;
    }

    private boolean applyCollectionCoverIfNeeded(VideoCollection collection, VideoFileScanner.VideoFileInfo file, User user) {
        if (collection.getCoverUrl() != null && !collection.getCoverUrl().isEmpty()) {
            return false;
        }
        Video video = getOrCreateVideo(file, user);
        if (video.getCoverUrl() == null || video.getCoverUrl().isEmpty()) {
            return false;
        }
        collection.setCoverUrl(video.getCoverUrl());
        collection.setUpdateId(user.getId());
        collection.setUpdateName(user.getUsername());
        collection.setUpdateTime(new Date());
        return true;
    }

    private void ensureDefaultCover(Video existing, VideoFileScanner.VideoFileInfo file, User user) {
        if (existing.getCoverUrl() != null && !existing.getCoverUrl().isEmpty()) {
            return;
        }
        String coverUrl = generateDefaultCover(file);
        if (coverUrl == null) {
            return;
        }
        existing.setCoverUrl(coverUrl);
        existing.setUpdateId(user.getId());
        existing.setUpdateName(user.getUsername());
        existing.setUpdateTime(new Date());
        videoRepository.updateById(existing);
    }

    private void ensureDefaultCover(Long videoId, VideoFileScanner.VideoFileInfo file, User user) {
        if (videoId == null) {
            return;
        }
        Video existing = videoRepository.getById(videoId);
        if (existing == null) {
            return;
        }
        ensureDefaultCover(existing, file, user);
    }

    private String generateDefaultCover(VideoFileScanner.VideoFileInfo file) {
        return VideoCoverUtil.extractFirstFrame(file.getFilePath(), uploadPath, DEFAULT_COVER_FOLDER, ffmpegPath);
    }

    private String buildVideoTitle(VideoFileScanner.VideoFileInfo file) {
        if (file == null) {
            return "未命名视频";
        }
        String fileName = file.getFileName();
        String originalName = stripExtension(fileName);
        if (originalName != null && !originalName.trim().isEmpty()) {
            return originalName.trim();
        }
        if (file.getTitle() != null && !file.getTitle().trim().isEmpty()) {
            return file.getTitle().trim();
        }
        return fileName == null || fileName.trim().isEmpty() ? "未命名视频" : fileName.trim();
    }

    private String stripExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    private Map<String, List<VideoFileScanner.VideoFileInfo>> groupByParentFolder(List<VideoFileScanner.VideoFileInfo> files) {
        Map<String, List<VideoFileScanner.VideoFileInfo>> map = new HashMap<>();
        for (VideoFileScanner.VideoFileInfo file : files) {
            String folderPath = getParentFolderPath(file.getFilePath());
            map.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(file);
        }
        return map;
    }

    private String getParentFolderPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            java.nio.file.Path parent = path.getParent();
            return parent == null ? "" : parent.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getFolderName(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return "";
        }
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(folderPath);
            java.nio.file.Path name = path.getFileName();
            return name == null ? "" : name.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isCollectionFolder(List<VideoFileScanner.VideoFileInfo> files, String folderName) {
        if (files == null || files.size() < 2) {
            return false;
        }
        if (videoFileScanner.hasMixedSeparateTags(files)) {
            return false;
        }
        int episodeCount = 0;
        int emptyTitleCount = 0;
        Map<String, Integer> titleCount = new HashMap<>();
        String normalizedFolder = normalizeTitle(folderName);

        for (VideoFileScanner.VideoFileInfo file : files) {
            if (file.getEpisodeNumber() != null && !file.getEpisodeNumber().isEmpty()) {
                episodeCount++;
            }
            String title = normalizeTitle(file.getTitle());
            if (title.isEmpty()) {
                emptyTitleCount++;
            } else {
                titleCount.put(title, titleCount.getOrDefault(title, 0) + 1);
            }
        }

        if (episodeCount < 2 || episodeCount < Math.ceil(files.size() * episodeHitRate)) {
            return false;
        }

        if (emptyTitleCount >= Math.ceil(files.size() * titleMatchRate)) {
            return true;
        }

        if (!normalizedFolder.isEmpty()) {
            int folderMatchCount = 0;
            for (String title : titleCount.keySet()) {
                if (title.equals(normalizedFolder) || title.contains(normalizedFolder) || normalizedFolder.contains(title)) {
                    folderMatchCount += titleCount.get(title);
                }
            }
            if (folderMatchCount >= Math.ceil(files.size() * titleMatchRate)) {
                return true;
            }
        }

        int maxCount = 0;
        for (Integer count : titleCount.values()) {
            if (count > maxCount) {
                maxCount = count;
            }
        }
        return maxCount >= Math.max(2, (int) Math.ceil(files.size() * titleMatchRate));
    }

    private String resolveCollectionTitle(List<VideoFileScanner.VideoFileInfo> files, String folderName) {
        if (folderName != null && !folderName.trim().isEmpty()) {
            return folderName.trim();
        }
        Map<String, Integer> titleCount = new HashMap<>();
        for (VideoFileScanner.VideoFileInfo file : files) {
            String title = file.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                titleCount.put(title, titleCount.getOrDefault(title, 0) + 1);
            }
        }
        String bestTitle = "";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : titleCount.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestTitle = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        if (!bestTitle.isEmpty()) {
            return bestTitle;
        }
        return files.isEmpty() ? "未命名合集" : files.get(0).getFileName();
    }

    private String resolveGroupTitle(List<VideoFileScanner.VideoFileInfo> files, String fallbackKey) {
        if (files != null && !files.isEmpty()) {
            VideoFileScanner.VideoFileInfo first = files.get(0);
            if (first.getTitle() != null && !first.getTitle().trim().isEmpty()) {
                return first.getTitle().trim();
            }
            if (first.getFileName() != null && !first.getFileName().trim().isEmpty()) {
                return first.getFileName().trim();
            }
        }
        return fallbackKey == null ? "未命名合集" : fallbackKey;
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("[\\[\\]()]", "")
                .replaceAll("[0-9\\s\\-_]", "")
                .toLowerCase()
                .trim();
    }
}
