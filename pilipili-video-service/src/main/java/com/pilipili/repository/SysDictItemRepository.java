package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.SysDictItem;
import com.pilipili.mapper.SysDictItemMapper;
import org.springframework.stereotype.Repository;

/**
 * 数据字典项Repository
 */
@Repository
public class SysDictItemRepository extends ServiceImpl<SysDictItemMapper, SysDictItem> {
}
