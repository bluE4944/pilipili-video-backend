package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.in.CreateTranscodeTaskRequest;
import com.pilipili.entity.out.TranscodeTaskItemVO;
import com.pilipili.entity.out.TranscodeTaskVO;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.repository.VideoRepository;
import com.pilipili.utils.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminTranscodeTaskService {

    private static final String TARGET_TYPE_VIDEO = "video";
    private static final String TARGET_TYPE_COLLECTION = "collection";
    private static final String OUTPUT_MODE_REPLACE = "replace_original";
    private static final String OUTPUT_MODE_SWITCH = "switch_path_only";
    private static final String TASK_STATUS_QUEUED = "queued";
    private static final String TASK_STATUS_RUNNING = "running";
    private static final String TASK_STATUS_SUCCESS = "success";
    private static final String TASK_STATUS_PARTIAL_SUCCESS = "partial_success";
    private static final String TASK_STATUS_FAILED = "failed";
    private static final String ITEM_STATUS_QUEUED = "queued";
    private static final String ITEM_STATUS_RUNNING = "running";
    private static final String ITEM_STATUS_SUCCESS = "success";
    private static final String ITEM_STATUS_SKIPPED = "skipped";
    private static final String ITEM_STATUS_FAILED = "failed";
    private static final int COMPLETED_TASK_CACHE_LIMIT = 200;
    private static final Set<String> FAST_AUDIO_CODECS = new HashSet<>(Arrays.asList("aac", "mp3", "mp2", "alac"));
    private static final Set<String> FAST_VIDEO_CODECS = new HashSet<>(Arrays.asList("h264", "avc1"));

    private final VideoRepository videoRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${video.transcode.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${video.transcode.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final AtomicLong taskIdGenerator = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentHashMap<Long, TaskState> taskStore = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Long> taskOrder = new ConcurrentLinkedDeque<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "admin-transcode-worker");
        thread.setDaemon(true);
        return thread;
    });

    public TranscodeTaskVO createTask(CreateTranscodeTaskRequest request, User operator) {
        validateRequest(request);
        List<ResolvedTaskItem> resolvedItems = resolveTaskItems(request.getTargetType(), request.getTargetIds());
        if (resolvedItems.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "No transcode candidates found");
        }

        TaskState task = new TaskState();
        task.taskId = taskIdGenerator.incrementAndGet();
        task.targetType = normalizeTargetType(request.getTargetType());
        task.outputMode = normalizeOutputMode(request.getOutputMode());
        task.status = TASK_STATUS_QUEUED;
        task.totalFileCount = resolvedItems.size();
        task.currentFileProgress = 0D;
        task.totalProgress = 0D;
        task.createTime = new Date();
        task.updateTime = task.createTime;
        task.operatorId = operator == null ? null : operator.getId();
        task.operatorName = operator == null ? null : operator.getUsername();

        for (int i = 0; i < resolvedItems.size(); i++) {
            ResolvedTaskItem resolvedItem = resolvedItems.get(i);
            TaskItemState item = new TaskItemState();
            item.index = i + 1;
            item.sourceValue = resolvedItem.sourceValue;
            item.sourcePath = resolvedItem.sourcePath;
            item.status = ITEM_STATUS_QUEUED;
            item.progress = 0D;
            item.videoRefs.addAll(resolvedItem.videoRefs);
            item.episodeRefs.addAll(resolvedItem.episodeRefs);
            task.items.add(item);
        }

        taskStore.put(task.taskId, task);
        taskOrder.addLast(task.taskId);
        executor.execute(() -> executeTask(task.taskId));
        pruneCompletedTasks();
        return toTaskVO(task, false);
    }

    public List<TranscodeTaskVO> listTasks() {
        List<TranscodeTaskVO> result = new ArrayList<>();
        List<Long> taskIds = new ArrayList<>(taskOrder);
        for (int i = taskIds.size() - 1; i >= 0; i--) {
            TaskState task = taskStore.get(taskIds.get(i));
            if (task != null) {
                result.add(toTaskVO(task, false));
            }
        }
        return result;
    }

    public TranscodeTaskVO getTaskDetail(Long taskId) {
        TaskState task = taskStore.get(taskId);
        if (task == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "Transcode task not found");
        }
        return toTaskVO(task, true);
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    private void validateRequest(CreateTranscodeTaskRequest request) {
        if (request == null) {
            throw new BusinessException(Status.PARAM_ERROR, "Request body is required");
        }
        if (!TARGET_TYPE_VIDEO.equals(normalizeTargetType(request.getTargetType()))
                && !TARGET_TYPE_COLLECTION.equals(normalizeTargetType(request.getTargetType()))) {
            throw new BusinessException(Status.PARAM_ERROR, "Unsupported targetType");
        }
        if (request.getTargetIds() == null || request.getTargetIds().isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "targetIds cannot be empty");
        }
        if (!OUTPUT_MODE_REPLACE.equals(normalizeOutputMode(request.getOutputMode()))
                && !OUTPUT_MODE_SWITCH.equals(normalizeOutputMode(request.getOutputMode()))) {
            throw new BusinessException(Status.PARAM_ERROR, "Unsupported outputMode");
        }
    }

    private List<ResolvedTaskItem> resolveTaskItems(String targetType, List<Long> targetIds) {
        String normalizedTargetType = normalizeTargetType(targetType);
        Set<Long> uniqueIds = new HashSet<>();
        for (Long targetId : targetIds) {
            if (targetId != null) {
                uniqueIds.add(targetId);
            }
        }
        if (uniqueIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ResolvedTaskItem> itemMap = new LinkedHashMap<>();
        if (TARGET_TYPE_VIDEO.equals(normalizedTargetType)) {
            List<Video> videos = videoRepository.listByIds(uniqueIds);
            for (Video video : videos) {
                if (video == null || video.getId() == null) {
                    continue;
                }
                addVideoItem(itemMap, video);
            }
            return new ArrayList<>(itemMap.values());
        }

        List<VideoEpisode> episodes = videoEpisodeRepository.list(
                new QueryWrapper<VideoEpisode>()
                        .in("collection_id", uniqueIds)
                        .orderByAsc("collection_id", "sort_order", "episode_number")
        );
        for (VideoEpisode episode : episodes) {
            if (episode == null || episode.getId() == null) {
                continue;
            }
            addEpisodeItem(itemMap, episode);
        }
        return new ArrayList<>(itemMap.values());
    }
    private void addVideoItem(Map<String, ResolvedTaskItem> itemMap, Video video) {
        String sourceValue = video.getVideoUrl();
        Path resolvedPath = resolveSourcePath(sourceValue);
        String key = resolvedPath == null ? "video:" + video.getId() : resolvedPath.toAbsolutePath().normalize().toString();
        ResolvedTaskItem item = itemMap.get(key);
        if (item == null) {
            item = new ResolvedTaskItem();
            item.sourceValue = sourceValue;
            item.sourcePath = resolvedPath;
            itemMap.put(key, item);
        }
        item.videoRefs.add(new VideoRef(video.getId()));
    }

    private void addEpisodeItem(Map<String, ResolvedTaskItem> itemMap, VideoEpisode episode) {
        String sourceValue = episode.getFilePath();
        Path resolvedPath = resolveSourcePath(sourceValue);
        String key = resolvedPath == null ? "episode:" + episode.getId() : resolvedPath.toAbsolutePath().normalize().toString();
        ResolvedTaskItem item = itemMap.get(key);
        if (item == null) {
            item = new ResolvedTaskItem();
            item.sourceValue = sourceValue;
            item.sourcePath = resolvedPath;
            itemMap.put(key, item);
        }
        item.episodeRefs.add(new EpisodeRef(episode.getId(), episode.getVideoId()));
    }

    private void executeTask(Long taskId) {
        TaskState task = taskStore.get(taskId);
        if (task == null) {
            return;
        }
        try {
            updateTaskStatus(task, TASK_STATUS_RUNNING);
            for (TaskItemState item : task.items) {
                runSingleItem(task, item);
            }
            finishTask(task);
        } catch (Exception e) {
            log.error("Execute transcode task failed, taskId={}", taskId, e);
            synchronized (task) {
                task.status = TASK_STATUS_FAILED;
                task.updateTime = new Date();
            }
        } finally {
            pruneCompletedTasks();
        }
    }

    private void runSingleItem(TaskState task, TaskItemState item) {
        synchronized (task) {
            item.status = ITEM_STATUS_RUNNING;
            item.progress = 0D;
            item.message = null;
            task.currentFile = displaySource(item);
            task.currentFileProgress = 0D;
            task.totalProgress = calculateTotalProgress(task, 0D);
            task.updateTime = new Date();
        }
        try {
            executeResolvedItem(task, item);
        } catch (Exception e) {
            log.error("Execute transcode item failed, taskId={}, source={}", task.taskId, displaySource(item), e);
            markItemFailed(task, item, e.getMessage() == null ? "Transcode failed" : e.getMessage());
        }
    }

    private void executeResolvedItem(TaskState task, TaskItemState item) throws IOException {
        Path sourcePath = item.sourcePath;
        if (sourcePath == null) {
            markItemFailed(task, item, "Only local files are supported");
            return;
        }
        if (!Files.exists(sourcePath)) {
            markItemFailed(task, item, "Source file does not exist");
            return;
        }
        if (Files.isDirectory(sourcePath)) {
            markItemFailed(task, item, "Source path is a directory");
            return;
        }
        if (!"mkv".equals(normalizeExtension(sourcePath.getFileName().toString()))) {
            markItemSkipped(task, item, "Only MKV files are processed");
            return;
        }

        MediaProbeResult probeResult = probeMedia(sourcePath);
        if (!FAST_VIDEO_CODECS.contains(probeResult.videoCodec)) {
            markItemSkipped(task, item, "Fast path only supports H.264/AVC video");
            return;
        }
        if (probeResult.audioCodec.isEmpty()) {
            markItemSkipped(task, item, "Audio stream not found");
            return;
        }
        if (FAST_AUDIO_CODECS.contains(probeResult.audioCodec)) {
            markItemSkipped(task, item, "Audio codec is already browser compatible");
            return;
        }

        Path finalOutputPath = buildFinalOutputPath(sourcePath);
        Path actualOutputPath = OUTPUT_MODE_REPLACE.equals(task.outputMode)
                ? buildTempOutputPath(finalOutputPath)
                : finalOutputPath;
        synchronized (task) {
            item.outputPath = finalOutputPath.toString();
            item.message = "Transcoding";
            task.updateTime = new Date();
        }

        executeFfmpeg(task, item, sourcePath, actualOutputPath, probeResult.durationSeconds);
        Path persistedPath = finalizeOutput(sourcePath, finalOutputPath, actualOutputPath, task.outputMode);
        syncDatabase(item, persistedPath, task.operatorId, task.operatorName);
        markItemSuccess(task, item, persistedPath.toString());
    }

    private MediaProbeResult probeMedia(Path sourcePath) {
        double durationSeconds = 0D;
        String durationOutput = runCommand(Arrays.asList(
                resolveFfprobeExecutable(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                sourcePath.toString()
        ), "probe duration");
        if (durationOutput != null && !durationOutput.trim().isEmpty()) {
            try {
                durationSeconds = Double.parseDouble(durationOutput.trim());
            } catch (NumberFormatException ignore) {
                durationSeconds = 0D;
            }
        }

        String videoCodec = probeFirstStreamCodec(sourcePath, "v:0");
        String audioCodec = probeFirstStreamCodec(sourcePath, "a:0");
        return new MediaProbeResult(durationSeconds, videoCodec, audioCodec);
    }

    private String probeFirstStreamCodec(Path sourcePath, String streamSelector) {
        String output = runCommand(Arrays.asList(
                resolveFfprobeExecutable(),
                "-v", "error",
                "-select_streams", streamSelector,
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                sourcePath.toString()
        ), "probe codec " + streamSelector);
        if (output == null || output.trim().isEmpty()) {
            return "";
        }
        String[] lines = output.split("\\R");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String normalized = line.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    private void executeFfmpeg(TaskState task, TaskItemState item, Path sourcePath, Path outputPath, double durationSeconds) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.exists(outputPath)) {
            Files.delete(outputPath);
        }

        List<String> command = new ArrayList<>();
        command.add(resolveFfmpegExecutable());
        command.add("-y");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        command.add("-i");
        command.add(sourcePath.toString());
        command.add("-map");
        command.add("0:v:0");
        command.add("-map");
        command.add("0:a?");
        command.add("-sn");
        command.add("-c:v");
        command.add("copy");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-ac");
        command.add("2");
        command.add("-movflags");
        command.add("+faststart");
        command.add(outputPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder outputCollector = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputCollector.append(line).append('\n');
                if (line.startsWith("out_time_ms=") && durationSeconds > 0) {
                    try {
                        long timeUs = Long.parseLong(line.substring("out_time_ms=".length()).trim());
                        updateItemProgress(task, item, Math.min((timeUs / 1000000D) / durationSeconds * 100D, 99.5D));
                    } catch (NumberFormatException ignore) {
                        // ignore invalid progress
                    }
                } else if (line.startsWith("out_time=") && durationSeconds > 0) {
                    double seconds = parseDuration(line.substring("out_time=".length()).trim());
                    if (seconds > 0) {
                        updateItemProgress(task, item, Math.min(seconds / durationSeconds * 100D, 99.5D));
                    }
                }
            }
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Transcode interrupted", e);
        }

        if (exitCode != 0) {
            throw new IOException("ffmpeg exited with code " + exitCode + ": " + abbreviate(outputCollector.toString(), 1200));
        }
        if (!Files.exists(outputPath)) {
            throw new IOException("Output file was not generated");
        }
        updateItemProgress(task, item, 100D);
    }

    private Path finalizeOutput(Path sourcePath, Path finalOutputPath, Path actualOutputPath, String outputMode) throws IOException {
        if (OUTPUT_MODE_SWITCH.equals(outputMode)) {
            return finalOutputPath;
        }
        if (Files.exists(finalOutputPath)) {
            Files.delete(finalOutputPath);
        }
        Files.move(actualOutputPath, finalOutputPath, StandardCopyOption.REPLACE_EXISTING);
        if (!sourcePath.equals(finalOutputPath) && Files.exists(sourcePath)) {
            Files.delete(sourcePath);
        }
        return finalOutputPath;
    }

    private void syncDatabase(TaskItemState item, Path finalPath, Long operatorId, String operatorName) throws IOException {
        long fileSize = Files.size(finalPath);
        FileTime lastModifiedTime = Files.getLastModifiedTime(finalPath);
        Date now = new Date();
        Date fileModifyTime = new Date(lastModifiedTime.toMillis());
        Set<Long> syncedVideoIds = new HashSet<>();

        for (VideoRef videoRef : item.videoRefs) {
            if (videoRef == null || videoRef.videoId == null || !syncedVideoIds.add(videoRef.videoId)) {
                continue;
            }
            Video video = videoRepository.getById(videoRef.videoId);
            if (video == null) {
                continue;
            }
            video.setVideoUrl(buildStoredPath(video.getVideoUrl(), finalPath));
            video.setFormat("mp4");
            video.setFileSize(fileSize);
            video.setUpdateId(operatorId);
            video.setUpdateName(operatorName);
            video.setUpdateTime(now);
            videoRepository.updateById(video);
        }

        for (EpisodeRef episodeRef : item.episodeRefs) {
            if (episodeRef == null || episodeRef.episodeId == null) {
                continue;
            }
            VideoEpisode episode = videoEpisodeRepository.getById(episodeRef.episodeId);
            if (episode == null) {
                continue;
            }
            episode.setFilePath(buildStoredPath(episode.getFilePath(), finalPath));
            episode.setFileFormat("mp4");
            episode.setFileSize(fileSize);
            episode.setFileModifyTime(fileModifyTime);
            episode.setUpdateId(operatorId);
            episode.setUpdateName(operatorName);
            episode.setUpdateTime(now);
            videoEpisodeRepository.updateById(episode);

            if (episode.getVideoId() != null && syncedVideoIds.add(episode.getVideoId())) {
                Video video = videoRepository.getById(episode.getVideoId());
                if (video != null) {
                    video.setVideoUrl(buildStoredPath(video.getVideoUrl(), finalPath));
                    video.setFormat("mp4");
                    video.setFileSize(fileSize);
                    video.setUpdateId(operatorId);
                    video.setUpdateName(operatorName);
                    video.setUpdateTime(now);
                    videoRepository.updateById(video);
                }
            }
        }
    }

    private String buildStoredPath(String originalValue, Path finalPath) {
        String sanitizedOriginal = stripQuery(originalValue);
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path normalizedTarget = finalPath.toAbsolutePath().normalize();
        if (sanitizedOriginal != null) {
            if (sanitizedOriginal.startsWith("/uploads/") || sanitizedOriginal.startsWith("uploads/")) {
                Path relative = relativizeIfPossible(uploadRoot, normalizedTarget);
                if (relative != null) {
                    String relativeValue = relative.toString().replace('\\', '/');
                    return sanitizedOriginal.startsWith("/uploads/") ? "/uploads/" + relativeValue : "uploads/" + relativeValue;
                }
            }
            Path originalPath = safePath(sanitizedOriginal);
            if (originalPath != null && originalPath.isAbsolute()) {
                return finalPath.toString();
            }
            if (sanitizedOriginal.startsWith("/")) {
                Path relative = relativizeIfPossible(uploadRoot, normalizedTarget);
                if (relative != null) {
                    return "/" + relative.toString().replace('\\', '/');
                }
            }
            if (!sanitizedOriginal.trim().isEmpty()) {
                Path relative = relativizeIfPossible(uploadRoot, normalizedTarget);
                if (relative != null) {
                    return relative.toString().replace('\\', '/');
                }
            }
        }
        return normalizedTarget.toString().replace('\\', '/');
    }

    private Path relativizeIfPossible(Path root, Path target) {
        try {
            if (target.startsWith(root)) {
                return root.relativize(target);
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private void updateItemProgress(TaskState task, TaskItemState item, double progress) {
        synchronized (task) {
            item.progress = roundProgress(progress);
            task.currentFileProgress = item.progress;
            task.totalProgress = calculateTotalProgress(task, progress);
            task.updateTime = new Date();
        }
    }

    private void markItemSuccess(TaskState task, TaskItemState item, String outputPath) {
        synchronized (task) {
            item.status = ITEM_STATUS_SUCCESS;
            item.progress = 100D;
            item.outputPath = outputPath;
            item.message = "Completed";
            task.successCount++;
            task.currentFileProgress = 100D;
            task.totalProgress = calculateTotalProgress(task, 100D);
            task.updateTime = new Date();
        }
        log.info("Transcode item success, taskId={}, source={}, output={}", task.taskId, displaySource(item), outputPath);
    }

    private void markItemSkipped(TaskState task, TaskItemState item, String message) {
        synchronized (task) {
            item.status = ITEM_STATUS_SKIPPED;
            item.progress = 100D;
            item.message = message;
            task.skippedCount++;
            task.currentFileProgress = 100D;
            task.totalProgress = calculateTotalProgress(task, 100D);
            task.updateTime = new Date();
        }
        log.info("Transcode item skipped, taskId={}, source={}, reason={}", task.taskId, displaySource(item), message);
    }

    private void markItemFailed(TaskState task, TaskItemState item, String message) {
        synchronized (task) {
            item.status = ITEM_STATUS_FAILED;
            item.progress = 100D;
            item.message = message;
            task.failedCount++;
            task.currentFileProgress = 100D;
            task.totalProgress = calculateTotalProgress(task, 100D);
            task.updateTime = new Date();
        }
        log.warn("Transcode item failed, taskId={}, source={}, reason={}", task.taskId, displaySource(item), message);
    }

    private void finishTask(TaskState task) {
        synchronized (task) {
            task.currentFile = null;
            task.currentFileProgress = 0D;
            task.totalProgress = 100D;
            task.status = resolveFinalStatus(task);
            task.updateTime = new Date();
        }
    }

    private void updateTaskStatus(TaskState task, String status) {
        synchronized (task) {
            task.status = status;
            task.updateTime = new Date();
        }
    }

    private String resolveFinalStatus(TaskState task) {
        if (task.successCount > 0) {
            if (task.failedCount == 0 && task.skippedCount == 0) {
                return TASK_STATUS_SUCCESS;
            }
            return TASK_STATUS_PARTIAL_SUCCESS;
        }
        if (task.failedCount > 0 && task.skippedCount == 0) {
            return TASK_STATUS_FAILED;
        }
        if (task.skippedCount > 0 || task.failedCount > 0) {
            return TASK_STATUS_PARTIAL_SUCCESS;
        }
        return TASK_STATUS_FAILED;
    }

    private double calculateTotalProgress(TaskState task, double currentItemProgress) {
        if (task.totalFileCount <= 0) {
            return 100D;
        }
        int completed = task.successCount + task.failedCount + task.skippedCount;
        double raw = ((completed + Math.max(0D, Math.min(currentItemProgress, 100D)) / 100D) / task.totalFileCount) * 100D;
        return roundProgress(Math.min(raw, 100D));
    }

    private double roundProgress(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private void pruneCompletedTasks() {
        int completedCount = 0;
        for (Long taskId : taskOrder) {
            TaskState task = taskStore.get(taskId);
            if (task != null && isFinishedTaskStatus(task.status)) {
                completedCount++;
            }
        }
        if (completedCount <= COMPLETED_TASK_CACHE_LIMIT) {
            return;
        }
        for (Long taskId : new ArrayList<>(taskOrder)) {
            if (completedCount <= COMPLETED_TASK_CACHE_LIMIT) {
                return;
            }
            TaskState task = taskStore.get(taskId);
            if (task == null) {
                taskOrder.remove(taskId);
                continue;
            }
            if (!isFinishedTaskStatus(task.status)) {
                continue;
            }
            taskStore.remove(taskId);
            taskOrder.remove(taskId);
            completedCount--;
        }
    }

    private boolean isFinishedTaskStatus(String status) {
        return TASK_STATUS_SUCCESS.equals(status)
                || TASK_STATUS_PARTIAL_SUCCESS.equals(status)
                || TASK_STATUS_FAILED.equals(status);
    }

    private TranscodeTaskVO toTaskVO(TaskState task, boolean includeItems) {
        synchronized (task) {
            TranscodeTaskVO vo = new TranscodeTaskVO();
            vo.setTaskId(task.taskId);
            vo.setTargetType(task.targetType);
            vo.setOutputMode(task.outputMode);
            vo.setStatus(task.status);
            vo.setTotalFileCount(task.totalFileCount);
            vo.setSuccessCount(task.successCount);
            vo.setFailedCount(task.failedCount);
            vo.setSkippedCount(task.skippedCount);
            vo.setCurrentFile(task.currentFile);
            vo.setCurrentFileProgress(task.currentFileProgress);
            vo.setTotalProgress(task.totalProgress);
            vo.setCreateTime(task.createTime);
            vo.setUpdateTime(task.updateTime);
            if (includeItems) {
                List<TranscodeTaskItemVO> items = new ArrayList<>();
                for (TaskItemState item : task.items) {
                    TranscodeTaskItemVO itemVO = new TranscodeTaskItemVO();
                    itemVO.setIndex(item.index);
                    itemVO.setSourcePath(displaySource(item));
                    itemVO.setOutputPath(item.outputPath);
                    itemVO.setStatus(item.status);
                    itemVO.setProgress(item.progress);
                    itemVO.setMessage(item.message);
                    items.add(itemVO);
                }
                vo.setItems(items);
            }
            return vo;
        }
    }

    private String displaySource(TaskItemState item) {
        return item.sourcePath != null ? item.sourcePath.toString() : item.sourceValue;
    }

    private String runCommand(List<String> command, String action) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output;
            try (InputStream inputStream = process.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                output = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(action + " failed with exit code " + exitCode + ": " + abbreviate(output, 1200));
            }
            return output == null ? "" : output.trim();
        } catch (IOException e) {
            throw new BusinessException(Status.BUSINESS_ERROR, action + " failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(Status.BUSINESS_ERROR, action + " was interrupted");
        }
    }

    private Path resolveSourcePath(String value) {
        String normalized = stripQuery(value);
        if (normalized == null || normalized.trim().isEmpty()) {
            return null;
        }
        String lowerCaseValue = normalized.toLowerCase(Locale.ROOT);
        if (lowerCaseValue.startsWith("http://") || lowerCaseValue.startsWith("https://")) {
            return null;
        }
        if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) {
            String relative = normalized.startsWith("/") ? normalized.substring(1) : normalized;
            relative = relative.substring("uploads/".length());
            return Paths.get(uploadPath).resolve(relative).toAbsolutePath().normalize();
        }
        Path path = safePath(normalized);
        if (path != null && path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        String relative = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        return Paths.get(uploadPath).resolve(relative).toAbsolutePath().normalize();
    }

    private Path safePath(String value) {
        try {
            return Paths.get(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String stripQuery(String value) {
        if (value == null) {
            return null;
        }
        int queryIndex = value.indexOf('?');
        String normalized = queryIndex >= 0 ? value.substring(0, queryIndex) : value;
        int hashIndex = normalized.indexOf('#');
        return hashIndex >= 0 ? normalized.substring(0, hashIndex) : normalized;
    }

    private String normalizeExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        String normalized = fileName.trim().toLowerCase(Locale.ROOT);
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index >= normalized.length() - 1) {
            return "";
        }
        return normalized.substring(index + 1);
    }

    private Path buildFinalOutputPath(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        String baseName = index >= 0 ? fileName.substring(0, index) : fileName;
        return sourcePath.resolveSibling(baseName + ".mp4");
    }

    private Path buildTempOutputPath(Path finalOutputPath) {
        String fileName = finalOutputPath.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        String baseName = index >= 0 ? fileName.substring(0, index) : fileName;
        return finalOutputPath.resolveSibling(baseName + ".tmp.mp4");
    }

    private String resolveFfmpegExecutable() {
        return ffmpegPath == null || ffmpegPath.trim().isEmpty() ? "ffmpeg" : ffmpegPath.trim();
    }

    private String resolveFfprobeExecutable() {
        return ffprobePath == null || ffprobePath.trim().isEmpty() ? "ffprobe" : ffprobePath.trim();
    }

    private double parseDuration(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0D;
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            return 0D;
        }
        try {
            double hour = Double.parseDouble(parts[0]);
            double minute = Double.parseDouble(parts[1]);
            double second = Double.parseDouble(parts[2]);
            return hour * 3600D + minute * 60D + second;
        } catch (NumberFormatException ignore) {
            return 0D;
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String normalizeTargetType(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOutputMode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static class TaskState {
        private Long taskId;
        private String targetType;
        private String outputMode;
        private String status;
        private int totalFileCount;
        private int successCount;
        private int failedCount;
        private int skippedCount;
        private String currentFile;
        private Double currentFileProgress;
        private Double totalProgress;
        private Date createTime;
        private Date updateTime;
        private Long operatorId;
        private String operatorName;
        private final List<TaskItemState> items = new ArrayList<>();
    }

    private static class TaskItemState {
        private Integer index;
        private String sourceValue;
        private Path sourcePath;
        private String outputPath;
        private String status;
        private Double progress;
        private String message;
        private final List<VideoRef> videoRefs = new ArrayList<>();
        private final List<EpisodeRef> episodeRefs = new ArrayList<>();
    }

    private static class ResolvedTaskItem {
        private String sourceValue;
        private Path sourcePath;
        private final List<VideoRef> videoRefs = new ArrayList<>();
        private final List<EpisodeRef> episodeRefs = new ArrayList<>();
    }

    private static class VideoRef {
        private final Long videoId;

        private VideoRef(Long videoId) {
            this.videoId = videoId;
        }
    }

    private static class EpisodeRef {
        private final Long episodeId;
        private final Long videoId;

        private EpisodeRef(Long episodeId, Long videoId) {
            this.episodeId = episodeId;
            this.videoId = videoId;
        }
    }

    private static class MediaProbeResult {
        private final double durationSeconds;
        private final String videoCodec;
        private final String audioCodec;

        private MediaProbeResult(double durationSeconds, String videoCodec, String audioCodec) {
            this.durationSeconds = durationSeconds;
            this.videoCodec = videoCodec == null ? "" : videoCodec.trim().toLowerCase(Locale.ROOT);
            this.audioCodec = audioCodec == null ? "" : audioCodec.trim().toLowerCase(Locale.ROOT);
        }
    }
}
