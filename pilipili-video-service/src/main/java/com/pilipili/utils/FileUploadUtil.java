package com.pilipili.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传工具类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
public class FileUploadUtil {

    /**
     * 允许的视频格式
     */
    private static final String[] ALLOWED_VIDEO_FORMATS = {"mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"};

    /**
     * 允许的图片格式
     */
    private static final String[] ALLOWED_IMAGE_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 验证文件格式
     */
    public static boolean isValidVideoFormat(String filename) {
        String extension = getFileExtension(filename);
        for (String format : ALLOWED_VIDEO_FORMATS) {
            if (format.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证图片格式
     */
    public static boolean isValidImageFormat(String filename) {
        String extension = getFileExtension(filename);
        for (String format : ALLOWED_IMAGE_FORMATS) {
            if (format.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 生成唯一文件名
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    /**
     * 保存文件到本地（临时实现，实际应使用对象存储）
     */
    public static String saveFileLocally(MultipartFile file, String uploadDir) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String filename = generateUniqueFileName(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(filename);

        // 保存文件
        file.transferTo(filePath.toFile());
        log.info("文件保存成功: {}", filePath.toString());

        return filename;
    }
}
