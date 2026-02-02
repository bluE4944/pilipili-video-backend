package com.pilipili.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 视频文件扫描工具类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Component
public class VideoFileScanner {

    /**
     * 支持的视频格式
     */
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mkv", "avi", "flv", "mov", "wmv", "rmvb", "rm", "3gp", "webm", "m4v", "ts", "mts"
    ));

    /**
     * 集数匹配模式（如：01, 02, 第1集, EP01等）
     */
    private static final Pattern EPISODE_PATTERN = Pattern.compile(
            "(?:第?([0-9]+)[集话话]|EP([0-9]+)|([0-9]{2,})|E([0-9]+))", Pattern.CASE_INSENSITIVE
    );

    /**
     * 扫描文件夹中的所有视频文件
     */
    public List<VideoFileInfo> scanFolder(String folderPath) {
        List<VideoFileInfo> videoFiles = new ArrayList<>();
        Path path = Paths.get(folderPath);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.warn("文件夹不存在或不是目录: {}", folderPath);
            return videoFiles;
        }

        try {
            Files.walk(path)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        String extension = getFileExtension(fileName);
                        if (VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
                            VideoFileInfo info = extractFileInfo(filePath);
                            if (info != null) {
                                videoFiles.add(info);
                            }
                        }
                    });
        } catch (IOException e) {
            log.error("扫描文件夹失败: {}", folderPath, e);
        }

        return videoFiles;
    }

    /**
     * 提取文件信息
     */
    private VideoFileInfo extractFileInfo(Path filePath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            String fileName = filePath.getFileName().toString();
            String nameWithoutExt = removeExtension(fileName);

            VideoFileInfo info = new VideoFileInfo();
            info.setFilePath(filePath.toString());
            info.setFileName(fileName);
            info.setFileSize(attrs.size());
            info.setFileModifyTime(new Date(attrs.lastModifiedTime().toMillis()));
            info.setFileFormat(getFileExtension(fileName));

            // 提取集数
            String episodeNumber = extractEpisodeNumber(nameWithoutExt);
            info.setEpisodeNumber(episodeNumber);

            // 提取标题（去除集数信息）
            String title = extractTitle(nameWithoutExt, episodeNumber);
            info.setTitle(title);

            return info;
        } catch (IOException e) {
            log.error("提取文件信息失败: {}", filePath, e);
            return null;
        }
    }

    /**
     * 提取集数
     */
    private String extractEpisodeNumber(String fileName) {
        Matcher matcher = EPISODE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.isEmpty()) {
                    // 格式化为两位数
                    int num = Integer.parseInt(group);
                    return String.format("%02d", num);
                }
            }
        }
        return null;
    }

    /**
     * 提取标题（去除集数信息）
     */
    private String extractTitle(String fileName, String episodeNumber) {
        String title = fileName;
        if (episodeNumber != null) {
            // 移除各种集数格式
            title = title.replaceAll("(?i)(?:第?" + episodeNumber + "[集话话]|EP" + episodeNumber + "|" + episodeNumber + "|E" + episodeNumber + ")", "");
            title = title.replaceAll("\\s*[-_\\s]+\\s*$", ""); // 移除末尾的分隔符
        }
        return title.trim();
    }

    /**
     * 检测相似文件名并分组（用于合集整合）
     */
    public Map<String, List<VideoFileInfo>> groupSimilarFiles(List<VideoFileInfo> videoFiles) {
        Map<String, List<VideoFileInfo>> groups = new HashMap<>();

        for (VideoFileInfo file : videoFiles) {
            String baseTitle = file.getTitle();
            if (baseTitle == null || baseTitle.isEmpty()) {
                baseTitle = removeExtension(file.getFileName());
            }

            // 查找相似的标题
            String groupKey = findSimilarGroup(baseTitle, groups.keySet());
            if (groupKey == null) {
                groupKey = baseTitle;
            }

            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(file);
        }

        // 过滤掉只有一个文件的组
        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 查找相似的组
     */
    private String findSimilarGroup(String title, Set<String> existingGroups) {
        for (String group : existingGroups) {
            if (isSimilarTitle(title, group)) {
                return group;
            }
        }
        return null;
    }

    /**
     * 判断两个标题是否相似
     */
    private boolean isSimilarTitle(String title1, String title2) {
        // 移除常见的前缀后缀
        String clean1 = cleanTitle(title1);
        String clean2 = cleanTitle(title2);

        // 计算相似度（简单实现：检查是否包含相同的关键词）
        if (clean1.length() < 3 || clean2.length() < 3) {
            return false;
        }

        // 提取关键词（去除数字和特殊字符）
        String keywords1 = extractKeywords(clean1);
        String keywords2 = extractKeywords(clean2);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return false;
        }

        // 检查关键词重叠度
        return calculateSimilarity(keywords1, keywords2) > 0.6;
    }

    /**
     * 清理标题
     */
    private String cleanTitle(String title) {
        return title.replaceAll("[\\[\\]()【】]", "").trim();
    }

    /**
     * 提取关键词
     */
    private String extractKeywords(String title) {
        return title.replaceAll("[0-9\\s\\-_]", "").toLowerCase();
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(String str1, String str2) {
        int maxLen = Math.max(str1.length(), str2.length());
        if (maxLen == 0) return 1.0;

        int commonChars = 0;
        String shorter = str1.length() < str2.length() ? str1 : str2;
        String longer = str1.length() >= str2.length() ? str1 : str2;

        for (char c : shorter.toCharArray()) {
            if (longer.indexOf(c) >= 0) {
                commonChars++;
            }
        }

        return (double) commonChars / maxLen;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 移除扩展名
     */
    private String removeExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * 视频文件信息
     */
    public static class VideoFileInfo {
        private String filePath;
        private String fileName;
        private Long fileSize;
        private Date fileModifyTime;
        private String fileFormat;
        private String title;
        private String episodeNumber;

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Date getFileModifyTime() { return fileModifyTime; }
        public void setFileModifyTime(Date fileModifyTime) { this.fileModifyTime = fileModifyTime; }
        public String getFileFormat() { return fileFormat; }
        public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getEpisodeNumber() { return episodeNumber; }
        public void setEpisodeNumber(String episodeNumber) { this.episodeNumber = episodeNumber; }
    }
}
