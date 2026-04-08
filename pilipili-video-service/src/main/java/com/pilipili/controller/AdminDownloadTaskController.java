package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.in.CreateDownloadMagnetTaskRequest;
import com.pilipili.entity.out.DownloadStatusVO;
import com.pilipili.entity.out.DownloadTaskVO;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.service.AdminDownloadTaskService;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/download")
@Api(value = "下载任务管理", tags = "下载任务管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminDownloadTaskController {

    private final AdminDownloadTaskService adminDownloadTaskService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "用户未登录");
    }

    @PostMapping("/tasks/magnet")
    @ApiOperation("创建磁力下载任务")
    public Result<DownloadTaskVO> createMagnetTask(@RequestBody CreateDownloadMagnetTaskRequest request) {
        return Result.build(adminDownloadTaskService.createMagnetTask(request, getCurrentUser()));
    }

    @PostMapping(value = "/tasks/torrent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation("创建种子下载任务")
    public Result<DownloadTaskVO> createTorrentTask(
            @RequestParam("torrentFile") MultipartFile torrentFile,
            @RequestParam("folderConfigId") Long folderConfigId,
            @RequestParam(value = "addPaused", required = false) Boolean addPaused) {
        return Result.build(adminDownloadTaskService.createTorrentTask(torrentFile, folderConfigId, addPaused, getCurrentUser()));
    }

    @GetMapping("/tasks")
    @ApiOperation("查询下载任务列表")
    public Result<List<DownloadTaskVO>> listTasks() {
        return Result.build(adminDownloadTaskService.listTasks());
    }

    @GetMapping("/tasks/{taskId}")
    @ApiOperation("查询下载任务详情")
    public Result<DownloadTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.build(adminDownloadTaskService.getTaskDetail(taskId));
    }

    @PostMapping("/tasks/{taskId}/pause")
    @ApiOperation("暂停下载任务")
    public Result<?> pauseTask(@PathVariable Long taskId) {
        adminDownloadTaskService.pauseTask(taskId, getCurrentUser());
        return Result.build();
    }

    @PostMapping("/tasks/{taskId}/resume")
    @ApiOperation("继续下载任务")
    public Result<?> resumeTask(@PathVariable Long taskId) {
        adminDownloadTaskService.resumeTask(taskId, getCurrentUser());
        return Result.build();
    }

    @DeleteMapping("/tasks/{taskId}")
    @ApiOperation("删除下载任务")
    public Result<?> deleteTask(@PathVariable Long taskId, @RequestParam(defaultValue = "false") boolean deleteFiles) {
        adminDownloadTaskService.deleteTask(taskId, deleteFiles);
        return Result.build();
    }

    @PostMapping("/tasks/{taskId}/retry-import")
    @ApiOperation("重试自动入库")
    public Result<DownloadTaskVO> retryImport(@PathVariable Long taskId) {
        return Result.build(adminDownloadTaskService.retryImport(taskId));
    }

    @GetMapping("/status")
    @ApiOperation("查询下载器状态")
    public Result<DownloadStatusVO> getStatus() {
        return Result.build(adminDownloadTaskService.getDownloadStatus());
    }
}
