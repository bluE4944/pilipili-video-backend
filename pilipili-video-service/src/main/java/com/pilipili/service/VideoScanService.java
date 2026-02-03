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
import com.pilipili.utils.VideoFileScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final VideoFileScanner videoFileScanner;
    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final VideoRepository videoRepository;
    private final LocalFolderConfigService localFolderConfigService;

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

            // 分组相似文件
            Map<String, List<VideoFileScanner.VideoFileInfo>> groups = videoFileScanner.groupSimilarFiles(videoFiles);
            log.info("识别到 {} 个视频合集", groups.size());

            // 创建合集和分集
            for (Map.Entry<String, List<VideoFileScanner.VideoFileInfo>> entry : groups.entrySet()) {
                createCollection(entry.getKey(), entry.getValue(), config.getFolderPath(), user);
            }

            // 处理单个视频文件（未分组的）
            List<VideoFileScanner.VideoFileInfo> singleFiles = videoFiles.stream()
                    .filter(file -> groups.values().stream()
                            .noneMatch(group -> group.contains(file)))
                    .collect(Collectors.toList());

            for (VideoFileScanner.VideoFileInfo file : singleFiles) {
                createSingleVideoCollection(file, config.getFolderPath(), user);
            }

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
            if (existingEpisode.getVideoId() == null) {
                Video video = getOrCreateVideo(file, user);
                existingEpisode.setVideoId(video.getId());
                existingEpisode.setUpdateTime(new Date());
                videoEpisodeRepository.updateById(existingEpisode);
            }
            return;
        }

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
     * 查找已存在的合集
     */
    private VideoCollection findExistingCollection(String title, String sourceFolder) {
        QueryWrapper<VideoCollection> wrapper = new QueryWrapper<>();
        wrapper.eq("title", title);
        wrapper.eq("source_folder_path", sourceFolder);
        return videoCollectionRepository.getOne(wrapper);
    }

    private Video getOrCreateVideo(VideoFileScanner.VideoFileInfo file, User user) {
        QueryWrapper<Video> wrapper = new QueryWrapper<>();
        wrapper.eq("video_url", file.getFilePath());
        Video existing = videoRepository.getOne(wrapper);
        if (existing != null) {
            return existing;
        }

        Video video = new Video();
        video.setTitle(buildVideoTitle(file));
        video.setDescription("本地扫描视频");
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

    private String buildVideoTitle(VideoFileScanner.VideoFileInfo file) {
        if (file.getEpisodeNumber() != null && file.getTitle() != null && !file.getTitle().isEmpty()) {
            return file.getTitle() + " 第" + file.getEpisodeNumber() + "集";
        }
        if (file.getTitle() != null && !file.getTitle().isEmpty()) {
            return file.getTitle();
        }
        return file.getFileName();
    }
}
