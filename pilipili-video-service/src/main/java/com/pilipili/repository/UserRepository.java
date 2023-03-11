package com.pilipili.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.entity.User;
import com.pilipili.mapper.UserMapper;
import org.springframework.stereotype.Repository;


/**
 * 用户 Repository
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 22:25
 */
@Repository
public class UserRepository extends ServiceImpl<UserMapper, User> {


}
