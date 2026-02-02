package com.pilipili.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储服务接口
 * 支持OSS/S3等对象存储的适配
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
public interface StorageService {

    /**
     * 上传文件
     * @param file 文件
     * @param folder 文件夹路径
     * @return 文件URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 上传文件（分片上传）
     * @param file 文件分片
     * @param folder 文件夹路径
     * @param chunkNumber 分片序号
     * @param totalChunks 总分片数
     * @param uploadId 上传ID
     * @return 上传结果
     */
    String uploadChunk(MultipartFile file, String folder, Integer chunkNumber, Integer totalChunks, String uploadId);

    /**
     * 完成分片上传
     * @param folder 文件夹路径
     * @param uploadId 上传ID
     * @param filename 文件名
     * @return 文件URL
     */
    String completeChunkUpload(String folder, String uploadId, String filename);

    /**
     * 删除文件
     * @param fileUrl 文件URL
     * @return 是否成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * 生成临时访问URL（带时效和防盗链）
     * @param fileUrl 文件URL
     * @param expireSeconds 过期时间（秒）
     * @return 临时访问URL
     */
    String generatePresignedUrl(String fileUrl, Long expireSeconds);
}
