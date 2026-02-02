package com.pilipili.controller;

import com.pilipili.entity.SystemConfig;
import com.pilipili.entity.out.Result;
import com.pilipili.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/system/config")
@Api(value = "系统配置控制器", tags = "系统配置")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取配置值
     */
    @GetMapping("/{configKey}")
    @ApiOperation("获取配置值")
    public Result<String> getConfigValue(@PathVariable String configKey) {
        String value = systemConfigService.getConfigValue(configKey);
        return Result.build(value);
    }

    /**
     * 设置配置值
     */
    @PostMapping
    @ApiOperation("设置配置值")
    public Result<?> setConfigValue(
            @RequestParam String configKey,
            @RequestParam String configValue,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer configType) {
        systemConfigService.setConfigValue(configKey, configValue, description, configType);
        return Result.build();
    }

    /**
     * 获取所有配置
     */
    @GetMapping("/all")
    @ApiOperation("获取所有配置")
    public Result<List<SystemConfig>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return Result.build(configs);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{configKey}")
    @ApiOperation("删除配置")
    public Result<?> deleteConfig(@PathVariable String configKey) {
        systemConfigService.deleteConfig(configKey);
        return Result.build();
    }
}
