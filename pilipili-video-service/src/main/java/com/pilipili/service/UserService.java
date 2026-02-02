package com.pilipili.service;

import com.pilipili.entity.User;
import com.pilipili.repository.UserRepository;
import com.pilipili.utils.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户服务类
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 23:02
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if(user != null){
            return user;
        }
        throw new UsernameNotFoundException("User '" + username + "' not found");
    }

    public void saveAdmin(String password){
        User admin = new User();
        admin.setUserName("admin");
        admin.setEmail("-1");
        admin.setNikeName("admin");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setAuthorization("admin");
        admin.setRole(Role.ROLE_ADMIN.getCode());
        admin.setCreateId(-1L);
        admin.setCreateName("sys");
        admin.setCreateTime(new Date());
        admin.setUpdateId(-1L);
        admin.setUpdateName("sys");
        admin.setUpdateTime(new Date());
        admin.setLogicDel(0);
        userRepository.save(admin);
    }

    public void saveUser(String password){
        // TODO: 实现普通用户保存逻辑
    }

    /**
     * 用户注册
     */
    public void register(User user) {
        // 检查用户名是否已存在
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            existingUser = userRepository.findByUsername(user.getEmail());
            if (existingUser != null) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        user.setCreateId(-1L);
        user.setCreateName("system");
        user.setUpdateId(-1L);
        user.setUpdateName("system");
        user.setUpdateTime(new Date());
        userRepository.save(user);
    }

    /**
     * 更新用户信息
     */
    public User updateUserInfo(Long userId, com.pilipili.entity.in.UserCondition userCondition) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (userCondition.getNikeName() != null) {
            user.setNikeName(userCondition.getNikeName());
        }
        if (userCondition.getEmail() != null) {
            user.setEmail(userCondition.getEmail());
        }
        if (userCondition.getPhone() != null) {
            user.setPhone(userCondition.getPhone());
        }
        if (userCondition.getSex() != null) {
            user.setGender(userCondition.getSex());
        }

        user.setUpdateId(userId);
        user.setUpdateName(user.getUsername());
        user.setUpdateTime(new Date());
        userRepository.updateById(user);
        return user;
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateId(userId);
        user.setUpdateName(user.getUsername());
        user.setUpdateTime(new Date());
        userRepository.updateById(user);
    }
}
