package com.pilipili.controller;

import com.pilipili.entity.Video;
import com.pilipili.entity.VideoCollection;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/cover")
@Api(value = "封面访问接口", tags = "封面访问")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CoverController {

    private final VideoRepository videoRepository;
    private final VideoCollectionRepository videoCollectionRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/video/{videoId}")
    @ApiOperation("获取视频封面")
    public void getVideoCover(
            @ApiParam(value = "视频ID", required = true, example = "1") @PathVariable Long videoId,
            HttpServletResponse response) throws IOException {
        Video video = videoRepository.getById(videoId);
        if (video == null || video.getCoverUrl() == null || video.getCoverUrl().isEmpty()) {
            writeDefaultCover(response);
            return;
        }
        writeCover(video.getCoverUrl(), response);
    }

    @GetMapping("/collection/{collectionId}")
    @ApiOperation("获取合集封面")
    public void getCollectionCover(
            @ApiParam(value = "合集ID", required = true, example = "1") @PathVariable Long collectionId,
            HttpServletResponse response) throws IOException {
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        if (collection == null || collection.getCoverUrl() == null || collection.getCoverUrl().isEmpty()) {
            writeDefaultCover(response);
            return;
        }
        writeCover(collection.getCoverUrl(), response);
    }

    private void writeCover(String coverUrl, HttpServletResponse response) throws IOException {
        String normalized = stripQuery(coverUrl);
        if (isRemoteUrl(normalized)) {
            response.sendRedirect(normalized);
            return;
        }

        Path coverPath = resolveLocalCoverPath(normalized);
        if (coverPath == null || !Files.exists(coverPath) || !Files.isRegularFile(coverPath)) {
            writeDefaultCover(response);
            return;
        }

        String contentType = Files.probeContentType(coverPath);
        if (contentType == null) {
            contentType = "image/jpeg";
        }
        response.setContentType(contentType);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream inputStream = Files.newInputStream(coverPath)) {
            StreamUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException e) {
            log.warn("封面输出失败: {}", coverUrl, e);
        }
    }

    private void writeDefaultCover(HttpServletResponse response) throws IOException {
        ClassPathResource resource = new ClassPathResource("static/cover-default.svg");
        if (!resource.exists()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "封面不存在");
            return;
        }
        response.setContentType("image/svg+xml");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "public, max-age=86400");
        try (InputStream inputStream = resource.getInputStream()) {
            StreamUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException e) {
            log.warn("默认封面输出失败", e);
        }
    }

    private Path resolveLocalCoverPath(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path candidate;
        if (url.startsWith("/uploads/") || url.startsWith("uploads/")) {
            String normalized = url.startsWith("/") ? url.substring(1) : url;
            normalized = normalized.substring("uploads/".length());
            candidate = root.resolve(normalized);
        } else {
            Path raw = Paths.get(url);
            candidate = raw.isAbsolute() ? raw : root.resolve(url);
        }
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(root)) {
            return null;
        }
        return normalizedCandidate;
    }

    private String stripQuery(String url) {
        int idx = url.indexOf("?");
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
