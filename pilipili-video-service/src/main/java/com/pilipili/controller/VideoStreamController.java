package com.pilipili.controller;

import com.pilipili.service.VideoPlayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 视频流媒体控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/video/stream")
@Api(value = "视频流媒体控制器", tags = "视频流媒体")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VideoStreamController {

    private final VideoPlayService videoPlayService;

    @GetMapping("/{videoId}")
    @ApiOperation("视频流媒体播放（支持 Range）")
    public void streamVideo(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        VideoPlayService.VideoStreamInfo streamInfo = videoPlayService.getVideoStreamInfo(videoId);
        if (streamInfo.getRemoteUrl() != null && !streamInfo.getRemoteUrl().isEmpty()) {
            response.sendRedirect(streamInfo.getRemoteUrl());
            return;
        }

        Path filePath = streamInfo.getFilePath();
        if (filePath == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "视频文件不存在");
            return;
        }

        streamLocalFile(filePath, request, response, resolveContentType(filePath), "videoId=" + videoId);
    }

    @GetMapping("/compatible/{videoId}.mp4")
    @ApiOperation("兼容 MP4 流媒体播放（支持 Range）")
    public void streamCompatibleVideo(
            @PathVariable Long videoId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.GONE.value(), "服务端兼容转码已禁用，请使用最新版前端播放");
    }

    @GetMapping("/hls/{videoId}/index.m3u8")
    @ApiOperation("HLS 播放列表")
    public void streamHlsPlaylist(@PathVariable Long videoId, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.GONE.value(), "服务端 HLS 转码已禁用，请使用最新版前端播放");
    }

    @GetMapping("/hls/{videoId}/{fileName:.+}")
    @ApiOperation("HLS 分片文件")
    public void streamHlsSegment(
            @PathVariable Long videoId,
            @PathVariable String fileName,
            HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.GONE.value(), "服务端 HLS 转码已禁用，请使用最新版前端播放");
    }

    private String resolveContentType(Path filePath) throws IOException {
        String contentType = Files.probeContentType(filePath);
        if (contentType != null && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        String fileName = filePath.getFileName() == null ? "" : filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".mkv")) {
            return "video/x-matroska";
        }
        if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (fileName.endsWith(".webm")) {
            return "video/webm";
        }
        if (fileName.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (fileName.endsWith(".ogg") || fileName.endsWith(".ogv")) {
            return "video/ogg";
        }
        return "application/octet-stream";
    }

    private void serveStaticFile(Path filePath, String contentType, HttpServletResponse response) throws IOException {
        if (filePath == null || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "文件不存在");
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            StreamUtils.copy(inputStream, response.getOutputStream());
        }
    }

    private void streamLocalFile(
            Path filePath,
            HttpServletRequest request,
            HttpServletResponse response,
            String contentType,
            String logKey) throws IOException {
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "视频文件不存在");
            return;
        }

        long fileLength = file.length();
        String range = request.getHeader("Range");
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType(contentType);

        if (range == null || !range.startsWith("bytes=")) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLengthLong(fileLength);
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                StreamUtils.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                log.warn("视频流输出失败: {}", logKey, e);
            }
            return;
        }

        long start = 0;
        long end = fileLength - 1;
        String[] parts = range.replace("bytes=", "").split("-", 2);
        try {
            if (parts.length > 0 && !parts[0].isEmpty()) {
                start = Long.parseLong(parts[0]);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            response.setHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }
        if (end >= fileLength) {
            end = fileLength - 1;
        }
        if (start > end || start < 0) {
            response.setHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }

        long contentLength = end - start + 1;
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setContentLengthLong(contentLength);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int len = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (len == -1) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, len);
                remaining -= len;
            }
        } catch (IOException e) {
            log.warn("视频流 Range 输出失败: {}, range={}", logKey, range, e);
        }
    }
}
