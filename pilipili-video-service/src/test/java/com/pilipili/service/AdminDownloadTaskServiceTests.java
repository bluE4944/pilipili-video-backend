package com.pilipili.service;

import com.pilipili.entity.DownloadTask;
import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.entity.User;
import com.pilipili.entity.in.CreateDownloadMagnetTaskRequest;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.DownloadTaskRepository;
import com.pilipili.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdminDownloadTaskServiceTests {

    @Mock
    private DownloadTaskRepository downloadTaskRepository;
    @Mock
    private LocalFolderConfigService localFolderConfigService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoScanService videoScanService;
    @Mock
    private QbittorrentClient qbittorrentClient;

    private AdminDownloadTaskService adminDownloadTaskService;

    @Before
    public void setUp() {
        adminDownloadTaskService = new AdminDownloadTaskService(
                downloadTaskRepository,
                localFolderConfigService,
                userRepository,
                videoScanService,
                qbittorrentClient
        );
        ReflectionTestUtils.setField(adminDownloadTaskService, "defaultCategory", "pilipili");
        ReflectionTestUtils.setField(adminDownloadTaskService, "pollIntervalSeconds", 5);
    }

    @Test
    public void createMagnetTaskShouldPersistTaskTagAndSavePath() {
        LocalFolderConfig folderConfig = buildFolderConfig(11L, 1, "D:/downloads");
        when(localFolderConfigService.getConfigById(11L)).thenReturn(folderConfig);

        CreateDownloadMagnetTaskRequest request = new CreateDownloadMagnetTaskRequest();
        request.setFolderConfigId(11L);
        request.setMagnetUrl("magnet:?xt=urn:btih:testhash&dn=Example.Name");
        request.setAddPaused(Boolean.TRUE);

        User operator = buildUser(101L, "admin");

        adminDownloadTaskService.createMagnetTask(request, operator);

        ArgumentCaptor<DownloadTask> captor = ArgumentCaptor.forClass(DownloadTask.class);
        verify(downloadTaskRepository).save(captor.capture());
        DownloadTask savedTask = captor.getValue();
        assertNotNull(savedTask.getId());
        assertTrue(savedTask.getTaskTag().startsWith("pilipili_task_"));
        assertEquals("D:/downloads", savedTask.getSavePath());
        assertEquals("pilipili", savedTask.getCategory());
        verify(qbittorrentClient).addMagnet(eq(request.getMagnetUrl()), eq("D:/downloads"), eq("pilipili"), eq(savedTask.getTaskTag()), eq(true));
    }

    @Test(expected = BusinessException.class)
    public void createMagnetTaskShouldRejectDisabledFolder() {
        when(localFolderConfigService.getConfigById(22L)).thenReturn(buildFolderConfig(22L, 0, "D:/downloads"));

        CreateDownloadMagnetTaskRequest request = new CreateDownloadMagnetTaskRequest();
        request.setFolderConfigId(22L);
        request.setMagnetUrl("magnet:?xt=urn:btih:testhash");

        adminDownloadTaskService.createMagnetTask(request, buildUser(1L, "admin"));
    }

    @Test
    public void syncTasksShouldAutoImportOnlyOnceWhenCompleted() {
        DownloadTask task = buildTask(300L, 12L, 88L);
        task.setTorrentHash("hash-001");
        task.setStatus(AdminDownloadTaskService.STATUS_DOWNLOADING);
        task.setAutoImportStatus(AdminDownloadTaskService.AUTO_IMPORT_PENDING);
        when(downloadTaskRepository.list(any())).thenReturn(Collections.singletonList(task));
        when(userRepository.getById(88L)).thenReturn(buildUser(88L, "creator"));

        QbittorrentClient.QbTorrentInfo info = new QbittorrentClient.QbTorrentInfo();
        info.setHash("hash-001");
        info.setName("示例种子");
        info.setState("uploading");
        info.setProgress(100D);
        info.setSavePath("D:/downloads");
        info.setDownloadedBytes(1024L);
        info.setTotalBytes(1024L);
        info.setDownloadSpeed(0L);
        info.setEtaSeconds(0L);
        when(qbittorrentClient.getTorrentByHash("hash-001")).thenReturn(info);

        adminDownloadTaskService.syncTasks();
        adminDownloadTaskService.syncTasks();

        verify(videoScanService, times(1)).scanAndCreateCollections(12L, userRepository.getById(88L));
        assertEquals(AdminDownloadTaskService.AUTO_IMPORT_SUCCESS, task.getAutoImportStatus());
        assertEquals(AdminDownloadTaskService.STATUS_COMPLETED, task.getStatus());
    }

    @Test
    public void retryImportShouldInvokeScanForCompletedTask() {
        DownloadTask task = buildTask(301L, 66L, 99L);
        task.setStatus(AdminDownloadTaskService.STATUS_COMPLETED);
        task.setAutoImportStatus(AdminDownloadTaskService.AUTO_IMPORT_FAILED);
        when(downloadTaskRepository.getById(301L)).thenReturn(task);
        User operator = buildUser(99L, "creator");
        when(userRepository.getById(99L)).thenReturn(operator);

        adminDownloadTaskService.retryImport(301L);

        verify(videoScanService).scanAndCreateCollections(66L, operator);
        assertEquals(AdminDownloadTaskService.AUTO_IMPORT_SUCCESS, task.getAutoImportStatus());
    }

    @Test
    public void deleteTaskShouldPassDeleteFilesFlag() {
        DownloadTask task = buildTask(302L, 66L, 99L);
        task.setTorrentHash("hash-del");
        when(downloadTaskRepository.getById(302L)).thenReturn(task);

        adminDownloadTaskService.deleteTask(302L, true);

        verify(qbittorrentClient).delete("hash-del", true);
        verify(downloadTaskRepository).removeById(302L);
    }

    @Test
    public void createTorrentTaskShouldUploadTorrentFile() {
        LocalFolderConfig folderConfig = buildFolderConfig(55L, 1, "D:/downloads");
        when(localFolderConfigService.getConfigById(55L)).thenReturn(folderConfig);
        MockMultipartFile file = new MockMultipartFile("torrentFile", "example.torrent", "application/x-bittorrent", "torrent".getBytes(StandardCharsets.UTF_8));

        adminDownloadTaskService.createTorrentTask(file, 55L, Boolean.FALSE, buildUser(9L, "admin"));

        verify(qbittorrentClient).addTorrent(any(byte[].class), eq("example.torrent"), eq("D:/downloads"), eq("pilipili"), anyString(), eq(false));
        verify(downloadTaskRepository, never()).removeById(anyLong());
    }

    private LocalFolderConfig buildFolderConfig(Long id, Integer enabled, String path) {
        LocalFolderConfig config = new LocalFolderConfig();
        config.setId(id);
        config.setEnabled(enabled);
        config.setFolderPath(path);
        config.setConfigName("下载目录");
        return config;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUserName(username);
        return user;
    }

    private DownloadTask buildTask(Long taskId, Long folderConfigId, Long createId) {
        DownloadTask task = new DownloadTask();
        task.setId(taskId);
        task.setTaskTag("pilipili_task_" + taskId);
        task.setFolderConfigId(folderConfigId);
        task.setCreateId(createId);
        task.setSavePath("D:/downloads");
        task.setCategory("pilipili");
        return task;
    }
}
