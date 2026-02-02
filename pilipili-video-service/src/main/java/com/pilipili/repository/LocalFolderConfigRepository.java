package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.LocalFolderConfig;
import com.pilipili.mapper.LocalFolderConfigMapper;
import org.springframework.stereotype.Repository;

/**
 * 本地文件夹配置Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Repository
public class LocalFolderConfigRepository extends ServiceImpl<LocalFolderConfigMapper, LocalFolderConfig> {
}
