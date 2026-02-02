package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pilipili.entity.SystemConfig;
import com.pilipili.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 系统配置服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * 获取配置值
     */
    public String getConfigValue(String configKey) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        SystemConfig config = systemConfigRepository.getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 设置配置值
     */
    @Transactional(rollbackFor = Exception.class)
    public void setConfigValue(String configKey, String configValue, String description, Integer configType) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        SystemConfig config = systemConfigRepository.getOne(wrapper);

        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setDescription(description);
            config.setConfigType(configType != null ? configType : 1);
            config.setCreateTime(new Date());
            config.setLogicDel(0);
            systemConfigRepository.save(config);
        } else {
            config.setConfigValue(configValue);
            config.setDescription(description);
            config.setUpdateTime(new Date());
            systemConfigRepository.updateById(config);
        }

        log.info("系统配置更新成功: configKey={}, configValue={}", configKey, configValue);
    }

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.list();
    }

    /**
     * 删除配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String configKey) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", configKey);
        systemConfigRepository.remove(wrapper);
        log.info("系统配置删除成功: configKey={}", configKey);
    }
}
