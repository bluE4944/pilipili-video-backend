package com.pilipili.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pilipili.domain.entity.User;
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

    /**
     * 根据用户名查找
     * @param username 用户名
     * @return 用户
     */
    public User findByUsername(String username){
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.lambda().eq(User::getUsername,username);
        userQueryWrapper.lambda().or().eq(User::getEmail,username);
        userQueryWrapper.lambda().or().eq(User::getPhone,username);
        userQueryWrapper.lambda().last("limit 1");
        return this.baseMapper.selectOne(userQueryWrapper);
    }


}
