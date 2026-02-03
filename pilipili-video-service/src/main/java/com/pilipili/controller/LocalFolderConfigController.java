package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.entity.User;
import com.pilipili.entity.out.Result;
import com.pilipili.service.LocalFolderConfigService;
import com.pilipili.service.VideoScanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 本地文件夹配置控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/local-folder")
@Api(value = "本地文件夹配置控制器", tags = "本地文件夹配置")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalFolderConfigController {

    private final LocalFolderConfigService localFolderConfigService;
    private final VideoScanService videoScanService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("用户未登录");
    }

    /**
     * 添加文件夹配置
     */
    @PostMapping("/config")
    @ApiOperation("添加文件夹配置")
    public Result<LocalFolderConfig> addConfig(@RequestBody LocalFolderConfig config) {
        LocalFolderConfig result = localFolderConfigService.addConfig(config);
        return Result.build(result);
    }

    /**
     * 更新文件夹配置
     */
    @PutMapping("/config/{id}")
    @ApiOperation("更新文件夹配置")
    public Result<LocalFolderConfig> updateConfig(@PathVariable Long id, @RequestBody LocalFolderConfig config) {
        config.setId(id);
        LocalFolderConfig result = localFolderConfigService.updateConfig(config);
        return Result.build(result);
    }

    /**
     * 删除文件夹配置
     */
    @DeleteMapping("/config/{id}")
    @ApiOperation("删除文件夹配置")
    public Result<?> deleteConfig(@PathVariable Long id) {
        localFolderConfigService.deleteConfig(id);
        return Result.build();
    }

    /**
     * 根据ID获取配置
     */
    @GetMapping("/config/{id}")
    @ApiOperation("根据ID获取配置")
    public Result<LocalFolderConfig> getConfigById(@PathVariable Long id) {
        LocalFolderConfig config = localFolderConfigService.getConfigById(id);
        return Result.build(config);
    }

    /**
     * 分页查询配置
     */
    @GetMapping("/config/page")
    @ApiOperation("分页查询配置")
    public Result<Page<LocalFolderConfig>> getConfigPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<LocalFolderConfig> page = localFolderConfigService.getConfigPage(pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 获取所有启用的配置
     */
    @GetMapping("/config/enabled")
    @ApiOperation("获取所有启用的配置")
    public Result<List<LocalFolderConfig>> getEnabledConfigs() {
        List<LocalFolderConfig> configs = localFolderConfigService.getEnabledConfigs();
        return Result.build(configs);
    }

    /**
     * 扫描文件夹
     */
    @PostMapping("/scan/{configId}")
    @ApiOperation("扫描文件夹")
    public Result<?> scanFolder(@PathVariable Long configId) {
        User user = getCurrentUser();
        videoScanService.scanAndCreateCollections(configId, user);
        return Result.build();
    }
}
