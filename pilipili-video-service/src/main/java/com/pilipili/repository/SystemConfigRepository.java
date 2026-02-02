package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.SystemConfig;
import com.pilipili.mapper.SystemConfigMapper;
import org.springframework.stereotype.Repository;

/**
 * 系统配置Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class SystemConfigRepository extends ServiceImpl<SystemConfigMapper, SystemConfig> {
}
