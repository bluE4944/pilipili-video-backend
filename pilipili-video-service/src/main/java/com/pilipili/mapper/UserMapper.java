package com.pilipili.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pilipili.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 mapper
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 22:26
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
