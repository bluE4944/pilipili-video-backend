package com.pilipili.service;

import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
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
    private final LocalFolderConfigService localFolderConfigService;

    /**
     * 扫描文件夹并创建合集
     */
    @Transactional(rollbackFor = Exception.class)
    public void scanAndCreateCollections(Long configId) {
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
                createCollection(entry.getKey(), entry.getValue(), config.getFolderPath());
            }

            // 处理单个视频文件（未分组的）
            List<VideoFileScanner.VideoFileInfo> singleFiles = videoFiles.stream()
                    .filter(file -> groups.values().stream()
                            .noneMatch(group -> group.contains(file)))
                    .collect(Collectors.toList());

            for (VideoFileScanner.VideoFileInfo file : singleFiles) {
                createSingleVideoCollection(file, config.getFolderPath());
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
    private void createCollection(String title, List<VideoFileScanner.VideoFileInfo> files, String sourceFolder) {
        // 检查是否已存在
        VideoCollection existing = findExistingCollection(title, sourceFolder);
        VideoCollection collection;
        
        if (existing != null) {
            collection = existing;
        } else {
            collection = new VideoCollection();
            collection.setTitle(title);
            collection.setDescription("自动整合的视频合集");
            collection.setSourceFolderPath(sourceFolder);
            collection.setCollectionType(1); // 自动整合
            collection.setEnabled(1);
            collection.setCreateTime(new Date());
            collection.setLogicDel(0);
            videoCollectionRepository.save(collection);
        }

        // 按集数排序
        files.sort((f1, f2) -> {
            String ep1 = f1.getEpisodeNumber() != null ? f1.getEpisodeNumber() : "999";
            String ep2 = f2.getEpisodeNumber() != null ? f2.getEpisodeNumber() : "999";
            return ep1.compareTo(ep2);
        });

        // 创建分集
        int sortOrder = 1;
        for (VideoFileScanner.VideoFileInfo file : files) {
            VideoEpisode episode = new VideoEpisode();
            episode.setCollectionId(collection.getId());
            episode.setEpisodeNumber(file.getEpisodeNumber());
            episode.setEpisodeName(file.getEpisodeNumber() != null ? 
                    "第" + file.getEpisodeNumber() + "集" : file.getTitle());
            episode.setFilePath(file.getFilePath());
            episode.setFileSize(file.getFileSize());
            episode.setFileModifyTime(file.getFileModifyTime());
            episode.setFileFormat(file.getFileFormat());
            episode.setSortOrder(sortOrder++);
            episode.setCreateTime(new Date());
            episode.setLogicDel(0);
            videoEpisodeRepository.save(episode);
        }

        // 更新合集视频数量
        collection.setVideoCount(files.size());
        collection.setUpdateTime(new Date());
        videoCollectionRepository.updateById(collection);

        log.info("创建合集成功: collectionId={}, title={}, episodeCount={}", 
                collection.getId(), title, files.size());
    }

    /**
     * 创建单个视频合集
     */
    private void createSingleVideoCollection(VideoFileScanner.VideoFileInfo file, String sourceFolder) {
        VideoCollection collection = new VideoCollection();
        collection.setTitle(file.getTitle());
        collection.setDescription("单个视频文件");
        collection.setSourceFolderPath(sourceFolder);
        collection.setCollectionType(1);
        collection.setVideoCount(1);
        collection.setEnabled(1);
        collection.setCreateTime(new Date());
        collection.setLogicDel(0);
        videoCollectionRepository.save(collection);

        VideoEpisode episode = new VideoEpisode();
        episode.setCollectionId(collection.getId());
        episode.setEpisodeName(file.getTitle());
        episode.setFilePath(file.getFilePath());
        episode.setFileSize(file.getFileSize());
        episode.setFileModifyTime(file.getFileModifyTime());
        episode.setFileFormat(file.getFileFormat());
        episode.setSortOrder(1);
        episode.setCreateTime(new Date());
        episode.setLogicDel(0);
        videoEpisodeRepository.save(episode);
    }

    /**
     * 查找已存在的合集
     */
    private VideoCollection findExistingCollection(String title, String sourceFolder) {
        // 简化实现：根据标题和文件夹路径查找
        // 实际可以根据更复杂的匹配规则
        return null; // TODO: 实现查找逻辑
    }
}
