package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.pilipili.entity.DownloadTask;
import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.entity.User;
import com.pilipili.entity.in.CreateDownloadMagnetTaskRequest;
import com.pilipili.entity.out.DownloadStatusVO;
import com.pilipili.entity.out.DownloadTaskVO;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.DownloadTaskRepository;
import com.pilipili.repository.UserRepository;
import com.pilipili.utils.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminDownloadTaskService {

    static final String SOURCE_TYPE_MAGNET = "magnet";
    static final String SOURCE_TYPE_TORRENT = "torrent";
    static final String STATUS_QUEUED = "queued";
    static final String STATUS_DOWNLOADING = "downloading";
    static final String STATUS_PAUSED = "paused";
    static final String STATUS_COMPLETED = "completed";
    static final String STATUS_FAILED = "failed";
    static final String AUTO_IMPORT_PENDING = "pending";
    static final String AUTO_IMPORT_RUNNING = "running";
    static final String AUTO_IMPORT_SUCCESS = "success";
    static final String AUTO_IMPORT_FAILED = "failed";

    private static final String TASK_TAG_PREFIX = "pilipili_task_";
    private static final Set<String> PAUSED_STATES = new HashSet<>(Arrays.asList("pauseddl", "pausedup"));
    private static final Set<String> FAILURE_STATES = new HashSet<>(Arrays.asList("error", "missingfiles"));
    private static final Set<String> COMPLETED_STATES = new HashSet<>(Arrays.asList("uploading", "stalledup", "queuedup", "forcedup", "pausedup", "checkingup"));

    private final DownloadTaskRepository downloadTaskRepository;
    private final LocalFolderConfigService localFolderConfigService;
    private final UserRepository userRepository;
    private final VideoScanService videoScanService;
    private final QbittorrentClient qbittorrentClient;

    @Value("${download.qb.default-category:pilipili}")
    private String defaultCategory;

    @Value("${download.qb.poll-interval-seconds:5}")
    private Integer pollIntervalSeconds;

    @Transactional(rollbackFor = Exception.class)
    public DownloadTaskVO createMagnetTask(CreateDownloadMagnetTaskRequest request, User operator) {
        if (request == null || isBlank(request.getMagnetUrl())) {
            throw new BusinessException(Status.PARAM_ERROR, "磁力链接不能为空");
        }
        LocalFolderConfig folderConfig = resolveEnabledFolderConfig(request.getFolderConfigId());
        DownloadTask task = createTaskSkeleton(SOURCE_TYPE_MAGNET, extractMagnetName(request.getMagnetUrl()), folderConfig, operator);
        downloadTaskRepository.save(task);
        try {
            qbittorrentClient.addMagnet(request.getMagnetUrl().trim(), task.getSavePath(), task.getCategory(), task.getTaskTag(), Boolean.TRUE.equals(request.getAddPaused()));
            return toTaskVO(task);
        } catch (RuntimeException e) {
            markTaskFailed(task, e.getMessage(), operator);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DownloadTaskVO createTorrentTask(MultipartFile torrentFile, Long folderConfigId, Boolean addPaused, User operator) {
        if (torrentFile == null || torrentFile.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "种子文件不能为空");
        }
        LocalFolderConfig folderConfig = resolveEnabledFolderConfig(folderConfigId);
        DownloadTask task = createTaskSkeleton(SOURCE_TYPE_TORRENT, torrentFile.getOriginalFilename(), folderConfig, operator);
        downloadTaskRepository.save(task);
        try {
            qbittorrentClient.addTorrent(torrentFile.getBytes(), safeFileName(torrentFile.getOriginalFilename()), task.getSavePath(), task.getCategory(), task.getTaskTag(), Boolean.TRUE.equals(addPaused));
            return toTaskVO(task);
        } catch (IOException e) {
            markTaskFailed(task, "读取种子文件失败: " + e.getMessage(), operator);
            throw new BusinessException(Status.BUSINESS_ERROR, "读取种子文件失败: " + e.getMessage());
        } catch (RuntimeException e) {
            markTaskFailed(task, e.getMessage(), operator);
            throw e;
        }
    }

    public List<DownloadTaskVO> listTasks() {
        QueryWrapper<DownloadTask> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return downloadTaskRepository.list(wrapper).stream().map(this::toTaskVO).collect(Collectors.toList());
    }

    public DownloadTaskVO getTaskDetail(Long taskId) {
        return toTaskVO(getTaskById(taskId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void pauseTask(Long taskId, User operator) {
        DownloadTask task = getTaskById(taskId);
        requireTorrentHash(task);
        qbittorrentClient.pause(task.getTorrentHash());
        task.setStatus(STATUS_PAUSED);
        task.setUpdateTime(new Date());
        updateOperator(task, operator);
        downloadTaskRepository.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resumeTask(Long taskId, User operator) {
        DownloadTask task = getTaskById(taskId);
        requireTorrentHash(task);
        qbittorrentClient.resume(task.getTorrentHash());
        task.setStatus(STATUS_DOWNLOADING);
        task.setUpdateTime(new Date());
        updateOperator(task, operator);
        downloadTaskRepository.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId, boolean deleteFiles) {
        DownloadTask task = getTaskById(taskId);
        if (!isBlank(task.getTorrentHash())) {
            qbittorrentClient.delete(task.getTorrentHash(), deleteFiles);
        }
        downloadTaskRepository.removeById(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DownloadTaskVO retryImport(Long taskId) {
        DownloadTask task = getTaskById(taskId);
        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(Status.BUSINESS_ERROR, "仅已完成的下载任务可重试入库");
        }
        runAutoImport(task);
        return toTaskVO(task);
    }

    public DownloadStatusVO getDownloadStatus() {
        DownloadStatusVO vo = new DownloadStatusVO();
        vo.setDefaultCategory(defaultCategory);
        vo.setPollIntervalSeconds(pollIntervalSeconds);
        vo.setLastCheckedAt(new Date());
        List<LocalFolderConfig> enabledConfigs = localFolderConfigService.getEnabledConfigs();
        if (!enabledConfigs.isEmpty()) {
            vo.setDefaultSavePath(enabledConfigs.get(0).getFolderPath());
        }
        try {
            vo.setVersion(qbittorrentClient.getAppVersion());
            vo.setConnected(Boolean.TRUE);
            vo.setMessage("连接正常");
        } catch (RuntimeException e) {
            vo.setConnected(Boolean.FALSE);
            vo.setMessage(e.getMessage());
        }
        return vo;
    }

    @Scheduled(fixedDelayString = "#{${download.qb.poll-interval-seconds:5} * 1000}")
    public void syncTasks() {
        QueryWrapper<DownloadTask> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("update_time");
        List<DownloadTask> tasks = downloadTaskRepository.list(wrapper);
        for (DownloadTask task : tasks) {
            try {
                syncSingleTask(task);
            } catch (Exception e) {
                log.warn("同步下载任务失败, taskId={}, message={}", task.getId(), e.getMessage());
            }
        }
    }

    private void syncSingleTask(DownloadTask task) {
        QbittorrentClient.QbTorrentInfo torrentInfo = findTorrentInfo(task);
        if (torrentInfo == null) {
            if (!isBlank(task.getTorrentHash()) && !STATUS_COMPLETED.equals(task.getStatus())) {
                task.setStatus(STATUS_FAILED);
                task.setErrorMessage("下载器中未找到任务");
                task.setLastSyncedAt(new Date());
                task.setUpdateTime(new Date());
                downloadTaskRepository.updateById(task);
            }
            return;
        }
        mergeTorrentInfo(task, torrentInfo);
        downloadTaskRepository.updateById(task);
        if (STATUS_COMPLETED.equals(task.getStatus()) && AUTO_IMPORT_PENDING.equals(task.getAutoImportStatus())) {
            runAutoImport(task);
        }
    }

    private QbittorrentClient.QbTorrentInfo findTorrentInfo(DownloadTask task) {
        if (!isBlank(task.getTorrentHash())) {
            QbittorrentClient.QbTorrentInfo byHash = qbittorrentClient.getTorrentByHash(task.getTorrentHash());
            if (byHash != null) {
                return byHash;
            }
        }
        if (isBlank(task.getTaskTag())) {
            return null;
        }
        List<QbittorrentClient.QbTorrentInfo> list = qbittorrentClient.getTorrentsByTag(task.getTaskTag());
        return list.isEmpty() ? null : list.get(0);
    }

    private void mergeTorrentInfo(DownloadTask task, QbittorrentClient.QbTorrentInfo torrentInfo) {
        task.setTorrentHash(blankToNull(torrentInfo.getHash()));
        task.setSourceName(firstNonBlank(torrentInfo.getName(), task.getSourceName()));
        task.setSavePath(firstNonBlank(torrentInfo.getSavePath(), task.getSavePath()));
        task.setQbtState(blankToNull(torrentInfo.getState()));
        task.setStatus(mapTaskStatus(torrentInfo.getState(), torrentInfo.getProgress()));
        task.setProgress(defaultDouble(torrentInfo.getProgress()));
        task.setDownloadedBytes(defaultLong(torrentInfo.getDownloadedBytes()));
        task.setTotalBytes(defaultLong(torrentInfo.getTotalBytes()));
        task.setDownloadSpeed(defaultLong(torrentInfo.getDownloadSpeed()));
        task.setEtaSeconds(defaultLong(torrentInfo.getEtaSeconds()));
        if (STATUS_FAILED.equals(task.getStatus())) {
            task.setErrorMessage(firstNonBlank(torrentInfo.getErrorMessage(), "下载失败"));
        } else if (isBlank(torrentInfo.getErrorMessage())) {
            task.setErrorMessage(null);
        } else {
            task.setErrorMessage(torrentInfo.getErrorMessage().trim());
        }
        task.setLastSyncedAt(new Date());
        task.setUpdateTime(new Date());
    }

    private void runAutoImport(DownloadTask task) {
        User operator = resolveOperator(task);
        task.setAutoImportStatus(AUTO_IMPORT_RUNNING);
        task.setAutoImportMessage("正在执行自动扫描入库");
        task.setUpdateTime(new Date());
        downloadTaskRepository.updateById(task);
        try {
            videoScanService.scanAndCreateCollections(task.getFolderConfigId(), operator);
            task.setAutoImportStatus(AUTO_IMPORT_SUCCESS);
            task.setAutoImportMessage("自动扫描入库完成");
        } catch (Exception e) {
            task.setAutoImportStatus(AUTO_IMPORT_FAILED);
            task.setAutoImportMessage(firstNonBlank(e.getMessage(), "自动扫描入库失败"));
            log.error("自动扫描入库失败, taskId={}", task.getId(), e);
        }
        task.setUpdateTime(new Date());
        downloadTaskRepository.updateById(task);
    }

    private DownloadTask createTaskSkeleton(String sourceType, String sourceName, LocalFolderConfig folderConfig, User operator) {
        Date now = new Date();
        DownloadTask task = new DownloadTask();
        Long taskId = IdWorker.getId();
        task.setId(taskId);
        task.setTaskTag(TASK_TAG_PREFIX + taskId);
        task.setSourceType(sourceType);
        task.setSourceName(blankToNull(sourceName));
        task.setFolderConfigId(folderConfig.getId());
        task.setSavePath(folderConfig.getFolderPath());
        task.setCategory(defaultCategory);
        task.setStatus(STATUS_QUEUED);
        task.setProgress(0D);
        task.setDownloadedBytes(0L);
        task.setTotalBytes(0L);
        task.setDownloadSpeed(0L);
        task.setEtaSeconds(0L);
        task.setAutoImportStatus(AUTO_IMPORT_PENDING);
        task.setCreateId(operator == null ? null : operator.getId());
        task.setCreateName(operator == null ? null : operator.getUsername());
        task.setCreateTime(now);
        task.setUpdateId(operator == null ? null : operator.getId());
        task.setUpdateName(operator == null ? null : operator.getUsername());
        task.setUpdateTime(now);
        task.setLogicDel(0);
        return task;
    }

    private LocalFolderConfig resolveEnabledFolderConfig(Long folderConfigId) {
        if (folderConfigId == null) {
            throw new BusinessException(Status.PARAM_ERROR, "folderConfigId 不能为空");
        }
        LocalFolderConfig folderConfig = localFolderConfigService.getConfigById(folderConfigId);
        if (folderConfig == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "文件夹配置不存在");
        }
        if (!Integer.valueOf(1).equals(folderConfig.getEnabled())) {
            throw new BusinessException(Status.BUSINESS_ERROR, "文件夹配置未启用");
        }
        if (isBlank(folderConfig.getFolderPath())) {
            throw new BusinessException(Status.BUSINESS_ERROR, "文件夹配置缺少保存路径");
        }
        return folderConfig;
    }

    private DownloadTask getTaskById(Long taskId) {
        DownloadTask task = downloadTaskRepository.getById(taskId);
        if (task == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "下载任务不存在");
        }
        return task;
    }

    private User resolveOperator(DownloadTask task) {
        if (task.getCreateId() == null) {
            throw new BusinessException(Status.BUSINESS_ERROR, "下载任务缺少创建人信息");
        }
        User user = userRepository.getById(task.getCreateId());
        if (user == null) {
            throw new BusinessException(Status.BUSINESS_ERROR, "下载任务创建人不存在");
        }
        return user;
    }

    private void requireTorrentHash(DownloadTask task) {
        if (isBlank(task.getTorrentHash())) {
            throw new BusinessException(Status.BUSINESS_ERROR, "任务尚未同步到 qBittorrent，稍后再试");
        }
    }

    private void markTaskFailed(DownloadTask task, String message, User operator) {
        task.setStatus(STATUS_FAILED);
        task.setErrorMessage(firstNonBlank(message, "创建下载任务失败"));
        task.setLastSyncedAt(new Date());
        task.setUpdateTime(new Date());
        updateOperator(task, operator);
        downloadTaskRepository.updateById(task);
    }

    private void updateOperator(DownloadTask task, User operator) {
        if (operator == null) {
            return;
        }
        task.setUpdateId(operator.getId());
        task.setUpdateName(operator.getUsername());
    }

    private DownloadTaskVO toTaskVO(DownloadTask task) {
        LocalFolderConfig folderConfig = task.getFolderConfigId() == null ? null : localFolderConfigService.getConfigById(task.getFolderConfigId());
        DownloadTaskVO vo = new DownloadTaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskTag(task.getTaskTag());
        vo.setTorrentHash(task.getTorrentHash());
        vo.setSourceType(task.getSourceType());
        vo.setSourceName(task.getSourceName());
        vo.setFolderConfigId(task.getFolderConfigId());
        vo.setFolderConfigName(folderConfig == null ? null : folderConfig.getConfigName());
        vo.setSavePath(task.getSavePath());
        vo.setCategory(task.getCategory());
        vo.setQbtState(task.getQbtState());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setDownloadedBytes(task.getDownloadedBytes());
        vo.setTotalBytes(task.getTotalBytes());
        vo.setDownloadSpeed(task.getDownloadSpeed());
        vo.setEtaSeconds(task.getEtaSeconds());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setAutoImportStatus(task.getAutoImportStatus());
        vo.setAutoImportMessage(task.getAutoImportMessage());
        vo.setLastSyncedAt(task.getLastSyncedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    static String mapTaskStatus(String qbtState, Double progress) {
        String normalized = qbtState == null ? "" : qbtState.trim().toLowerCase(Locale.ROOT);
        if (FAILURE_STATES.contains(normalized)) {
            return STATUS_FAILED;
        }
        if (PAUSED_STATES.contains(normalized)) {
            return STATUS_PAUSED;
        }
        if (COMPLETED_STATES.contains(normalized) || (progress != null && progress >= 100D)) {
            return STATUS_COMPLETED;
        }
        if (normalized.contains("queued")) {
            return STATUS_QUEUED;
        }
        return STATUS_DOWNLOADING;
    }

    private String extractMagnetName(String magnetUrl) {
        if (magnetUrl == null) {
            return null;
        }
        int nameIndex = magnetUrl.indexOf("dn=");
        if (nameIndex < 0) {
            return magnetUrl.trim();
        }
        String value = magnetUrl.substring(nameIndex + 3);
        int endIndex = value.indexOf('&');
        if (endIndex >= 0) {
            value = value.substring(0, endIndex);
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String safeFileName(String value) {
        return isBlank(value) ? "download.torrent" : value.trim();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        return blankToNull(second);
    }

    private Double defaultDouble(Double value) {
        return value == null ? 0D : value;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
