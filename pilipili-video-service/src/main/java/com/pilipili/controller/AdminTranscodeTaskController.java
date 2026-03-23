package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.in.CreateTranscodeTaskRequest;
import com.pilipili.entity.out.Result;
import com.pilipili.entity.out.TranscodeTaskVO;
import com.pilipili.exception.BusinessException;
import com.pilipili.service.AdminTranscodeTaskService;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/transcode/tasks")
@Api(value = "Admin transcode tasks", tags = "Admin transcode tasks")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminTranscodeTaskController {

    private final AdminTranscodeTaskService adminTranscodeTaskService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "User is not logged in");
    }

    @PostMapping
    @ApiOperation("Create a transcode task")
    public Result<TranscodeTaskVO> createTask(@RequestBody CreateTranscodeTaskRequest request) {
        return Result.build(adminTranscodeTaskService.createTask(request, getCurrentUser()));
    }

    @GetMapping
    @ApiOperation("List transcode tasks")
    public Result<List<TranscodeTaskVO>> listTasks() {
        return Result.build(adminTranscodeTaskService.listTasks());
    }

    @GetMapping("/{taskId}")
    @ApiOperation("Get transcode task detail")
    public Result<TranscodeTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.build(adminTranscodeTaskService.getTaskDetail(taskId));
    }
}
