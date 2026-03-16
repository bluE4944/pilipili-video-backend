package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.SysDict;
import com.pilipili.mapper.SysDictMapper;
import org.springframework.stereotype.Repository;

/**
 * 数据字典Repository
 */
@Repository
public class SysDictRepository extends ServiceImpl<SysDictMapper, SysDict> {
}
