package com.pilipili.service.impl;

import com.pilipili.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地存储服务实现（临时实现，实际应使用OSS/S3）
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // 存储分片上传信息
    private final ConcurrentHashMap<String, ChunkUploadInfo> chunkUploadMap = new ConcurrentHashMap<>();

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path folderPath = Paths.get(uploadPath, folder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            Path filePath = folderPath.resolve(filename);
            file.transferTo(filePath.toFile());
            log.info("文件上传成功: {}", filePath.toString());
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public String uploadChunk(MultipartFile file, String folder, Integer chunkNumber, Integer totalChunks, String uploadId) {
        try {
            ChunkUploadInfo info = chunkUploadMap.computeIfAbsent(uploadId, k -> new ChunkUploadInfo(folder, totalChunks));
            Path chunkPath = Paths.get(uploadPath, "chunks", uploadId);
            if (!Files.exists(chunkPath)) {
                Files.createDirectories(chunkPath);
            }
            Path chunkFile = chunkPath.resolve(chunkNumber.toString());
            file.transferTo(chunkFile.toFile());
            info.addChunk(chunkNumber);
            log.info("分片上传成功: uploadId={}, chunkNumber={}", uploadId, chunkNumber);
            return "success";
        } catch (IOException e) {
            log.error("分片上传失败", e);
            throw new RuntimeException("分片上传失败", e);
        }
    }

    @Override
    public String completeChunkUpload(String folder, String uploadId, String filename) {
        try {
            ChunkUploadInfo info = chunkUploadMap.get(uploadId);
            if (info == null || !info.isComplete()) {
                throw new RuntimeException("分片上传未完成");
            }
            Path chunkPath = Paths.get(uploadPath, "chunks", uploadId);
            Path folderPath = Paths.get(uploadPath, folder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            Path finalFile = folderPath.resolve(filename);
            // 合并分片（简化实现，实际应使用更高效的方式）
            Files.createFile(finalFile);
            for (int i = 0; i < info.getTotalChunks(); i++) {
                Path chunkFile = chunkPath.resolve(String.valueOf(i));
                if (Files.exists(chunkFile)) {
                    Files.write(finalFile, Files.readAllBytes(chunkFile), java.nio.file.StandardOpenOption.APPEND);
                }
            }
            // 清理分片文件
            deleteDirectory(chunkPath.toFile());
            chunkUploadMap.remove(uploadId);
            log.info("分片上传完成: {}", finalFile.toString());
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            log.error("分片合并失败", e);
            throw new RuntimeException("分片合并失败", e);
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(uploadPath).resolve(relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", fileUrl);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("文件删除失败", e);
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl, Long expireSeconds) {
        // 本地存储实现：直接返回文件URL，实际应生成带签名的临时URL
        // TODO: 实现防盗链和时效控制
        return fileUrl + "?expire=" + (System.currentTimeMillis() / 1000 + expireSeconds);
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private static class ChunkUploadInfo {
        private final String folder;
        private final int totalChunks;
        private final java.util.Set<Integer> uploadedChunks = new java.util.HashSet<>();

        public ChunkUploadInfo(String folder, int totalChunks) {
            this.folder = folder;
            this.totalChunks = totalChunks;
        }

        public void addChunk(int chunkNumber) {
            uploadedChunks.add(chunkNumber);
        }

        public boolean isComplete() {
            return uploadedChunks.size() == totalChunks;
        }

        public int getTotalChunks() {
            return totalChunks;
        }
    }
}
