package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.DownloadTask;
import com.pilipili.mapper.DownloadTaskMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DownloadTaskRepository extends ServiceImpl<DownloadTaskMapper, DownloadTask> {
}
