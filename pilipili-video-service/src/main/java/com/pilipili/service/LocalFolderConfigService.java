package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.repository.LocalFolderConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 本地文件夹配置服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalFolderConfigService {

    private final LocalFolderConfigRepository localFolderConfigRepository;

    /**
     * 添加文件夹配置
     */
    @Transactional(rollbackFor = Exception.class)
    public LocalFolderConfig addConfig(LocalFolderConfig config) {
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        config.setLogicDel(0);
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getScanStatus() == null) {
            config.setScanStatus(0);
        }
        localFolderConfigRepository.save(config);
        log.info("添加文件夹配置成功: id={}, path={}", config.getId(), config.getFolderPath());
        return config;
    }

    /**
     * 更新文件夹配置
     */
    @Transactional(rollbackFor = Exception.class)
    public LocalFolderConfig updateConfig(LocalFolderConfig config) {
        LocalFolderConfig existing = localFolderConfigRepository.getById(config.getId());
        if (existing == null) {
            throw new RuntimeException("配置不存在");
        }
        config.setUpdateTime(new Date());
        localFolderConfigRepository.updateById(config);
        log.info("更新文件夹配置成功: id={}", config.getId());
        return config;
    }

    /**
     * 删除文件夹配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        localFolderConfigRepository.removeById(id);
        log.info("删除文件夹配置成功: id={}", id);
    }

    /**
     * 根据ID获取配置
     */
    public LocalFolderConfig getConfigById(Long id) {
        return localFolderConfigRepository.getById(id);
    }

    /**
     * 分页查询配置
     */
    public Page<LocalFolderConfig> getConfigPage(Integer pageNum, Integer pageSize) {
        Page<LocalFolderConfig> page = new Page<>(pageNum, pageSize);
        QueryWrapper<LocalFolderConfig> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return localFolderConfigRepository.page(page, wrapper);
    }

    /**
     * 获取所有启用的配置
     */
    public List<LocalFolderConfig> getEnabledConfigs() {
        QueryWrapper<LocalFolderConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", 1);
        wrapper.orderByAsc("create_time");
        return localFolderConfigRepository.list(wrapper);
    }

    /**
     * 更新扫描状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateScanStatus(Long id, Integer status, Date scanTime) {
        LocalFolderConfig config = localFolderConfigRepository.getById(id);
        if (config != null) {
            config.setScanStatus(status);
            config.setLastScanTime(scanTime);
            config.setUpdateTime(new Date());
            localFolderConfigRepository.updateById(config);
        }
    }
}
