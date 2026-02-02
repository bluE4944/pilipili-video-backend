package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.in.UserCondition;
import com.pilipili.entity.out.Result;
import com.pilipili.service.UserService;
import com.pilipili.utils.Role;
import com.pilipili.utils.RSAUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.Date;

/**
 * 用户控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Api(value = "用户控制器", tags = "用户管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<User> register(@RequestBody UserCondition userCondition) {
        try {
            // RSA解密密码（如果前端加密了）
            String password = userCondition.getPassword();
            try {
                password = RSAUtil.decrypt(URLDecoder.decode(password, "UTF-8"), RSAUtil.private_key);
            } catch (Exception e) {
                log.debug("密码未加密或解密失败，使用原密码");
            }

            User user = new User();
            user.setUserName(userCondition.getUserName());
            user.setPassword(passwordEncoder.encode(password));
            user.setNikeName(userCondition.getNikeName() != null ? userCondition.getNikeName() : userCondition.getUserName());
            user.setEmail(userCondition.getEmail());
            user.setPhone(userCondition.getPhone());
            user.setGender(userCondition.getSex());
            user.setRole(Role.ROLE_USER.getCode());
            user.setAuthorization("user");
            user.setCreateTime(new Date());
            user.setLogicDel(0);

            userService.register(user);
            log.info("用户注册成功: userName={}", userCondition.getUserName());
            return Result.build(user);
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return Result.build(com.pilipili.utils.Status.SYSTEM_ERROR, "注册失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @ApiOperation("获取当前用户信息")
    public Result<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            // 清除敏感信息
            user.setPassword(null);
            return Result.build(user);
        }
        return Result.build(com.pilipili.utils.Status.UNAUTHORIZED, "用户未登录");
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    @ApiOperation("更新用户信息")
    public Result<User> updateUserInfo(@RequestBody UserCondition userCondition) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return Result.build(com.pilipili.utils.Status.UNAUTHORIZED, "用户未登录");
        }

        User currentUser = (User) authentication.getPrincipal();
        User user = userService.updateUserInfo(currentUser.getId(), userCondition);
        return Result.build(user);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @ApiOperation("修改密码")
    public Result<?> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return Result.build(com.pilipili.utils.Status.UNAUTHORIZED, "用户未登录");
        }

        User currentUser = (User) authentication.getPrincipal();
        userService.changePassword(currentUser.getId(), oldPassword, newPassword);
        return Result.build();
    }

    /**
     * 用户注销
     * 注意：JWT是无状态的，服务端无法主动使Token失效
     * 客户端需要删除本地存储的Token
     * 如果需要服务端控制，可以使用Token黑名单机制（存储在Redis中）
     */
    @PostMapping("/logout")
    @ApiOperation("用户注销")
    public Result<?> logout() {
        // JWT是无状态的，服务端清除Security上下文即可
        // 客户端需要删除本地存储的Token
        SecurityContextHolder.clearContext();
        log.info("用户注销成功");
        return Result.build();
    }
}
