package com.pilipili.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public final class VideoCoverUtil {

    private VideoCoverUtil() {
    }

    public static String extractFirstFrame(String videoPath, String uploadRootPath, String coverFolder, String ffmpegPath) {
        if (videoPath == null || videoPath.trim().isEmpty()) {
            return null;
        }
        if (uploadRootPath == null || uploadRootPath.trim().isEmpty()) {
            return null;
        }
        if (coverFolder == null || coverFolder.trim().isEmpty()) {
            return null;
        }

        String ffmpeg = (ffmpegPath == null || ffmpegPath.trim().isEmpty()) ? "ffmpeg" : ffmpegPath;
        Path videoFile = Paths.get(videoPath);
        if (!Files.exists(videoFile)) {
            log.warn("video file not found for cover extract: {}", videoPath);
            return null;
        }

        Path coverDir = Paths.get(uploadRootPath, coverFolder);
        try {
            Files.createDirectories(coverDir);
        } catch (IOException e) {
            log.warn("create cover directory failed: {}", coverDir, e);
            return null;
        }

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path outputPath = coverDir.resolve(fileName);

        List<String> command = Arrays.asList(
                ffmpeg,
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                videoFile.toString(),
                "-frames:v",
                "1",
                "-q:v",
                "2",
                outputPath.toString()
        );

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            drain(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(outputPath)) {
                Files.deleteIfExists(outputPath);
                log.warn("ffmpeg cover extract failed: exitCode={}, video={}", exitCode, videoPath);
                return null;
            }
        } catch (IOException e) {
            log.warn("ffmpeg cover extract failed: {}", videoPath, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ffmpeg cover extract interrupted: {}", videoPath, e);
            return null;
        }

        return "/uploads/" + coverFolder + "/" + fileName;
    }

    private static void drain(InputStream inputStream) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(inputStream)) {
            byte[] buffer = new byte[8192];
            while (in.read(buffer) != -1) {
                // drain
            }
        }
    }
}
