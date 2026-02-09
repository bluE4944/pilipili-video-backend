package com.pilipili.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.in.UserCondition;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.UserRepository;
import com.pilipili.utils.Role;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 管理员用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Api(value = "管理员用户管理", tags = "管理员-用户管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "用户未登录");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询用户")
    public Result<Page<User>> getUserPage(
            @ModelAttribute UserCondition condition,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (condition != null) {
            if (notBlank(condition.getUserName())) {
                wrapper.lambda().like(User::getUsername, condition.getUserName());
            }
            if (notBlank(condition.getNikeName())) {
                wrapper.lambda().like(User::getNikeName, condition.getNikeName());
            }
            if (notBlank(condition.getEmail())) {
                wrapper.lambda().like(User::getEmail, condition.getEmail());
            }
            if (notBlank(condition.getPhone())) {
                wrapper.lambda().like(User::getPhone, condition.getPhone());
            }
            if (notBlank(condition.getRole())) {
                wrapper.lambda().eq(User::getRole, condition.getRole());
            }
            if (notBlank(condition.getAuthorization())) {
                wrapper.lambda().eq(User::getAuthorization, condition.getAuthorization());
            }
        }
        wrapper.orderByDesc("create_time");
        Page<User> result = userRepository.page(page, wrapper);
        if (result.getRecords() != null) {
            for (User user : result.getRecords()) {
                sanitizeUser(user);
            }
        }
        return Result.build(result);
    }

    @GetMapping("/{userId}")
    @ApiOperation("获取用户详情")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "用户不存在");
        }
        sanitizeUser(user);
        return Result.build(user);
    }

    @PostMapping
    @ApiOperation("创建用户")
    public Result<User> createUser(@RequestBody UserCondition condition) {
        if (condition == null || !notBlank(condition.getUserName()) || !notBlank(condition.getPassword())) {
            throw new BusinessException(Status.PARAM_ERROR, "用户名和密码不能为空");
        }
        checkUserUnique(condition.getUserName(), condition.getEmail(), condition.getPhone(), null);

        User admin = getCurrentUser();
        User user = new User();
        user.setUserName(condition.getUserName());
        user.setPassword(passwordEncoder.encode(condition.getPassword()));
        user.setNikeName(notBlank(condition.getNikeName()) ? condition.getNikeName() : condition.getUserName());
        user.setEmail(condition.getEmail());
        user.setPhone(condition.getPhone());
        user.setGender(condition.getSex());
        String role = notBlank(condition.getRole()) ? condition.getRole() : Role.ROLE_USER.getCode();
        user.setRole(role);
        user.setAuthorization(notBlank(condition.getAuthorization()) ? condition.getAuthorization() : role);
        user.setCreateId(admin.getId());
        user.setCreateName(admin.getUsername());
        user.setCreateTime(new Date());
        user.setUpdateId(admin.getId());
        user.setUpdateName(admin.getUsername());
        user.setUpdateTime(new Date());
        user.setLogicDel(0);
        userRepository.save(user);
        sanitizeUser(user);
        return Result.build(user);
    }

    @PutMapping("/{userId}")
    @ApiOperation("更新用户信息")
    public Result<User> updateUser(@PathVariable Long userId, @RequestBody UserCondition condition) {
        User existing = userRepository.getById(userId);
        if (existing == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "用户不存在");
        }
        User admin = getCurrentUser();

        if (condition != null) {
            if (notBlank(condition.getUserName()) && !condition.getUserName().equals(existing.getUsername())) {
                checkUserUnique(condition.getUserName(), null, null, existing.getId());
                existing.setUserName(condition.getUserName());
            }
            if (notBlank(condition.getNikeName())) {
                existing.setNikeName(condition.getNikeName());
            }
            if (notBlank(condition.getEmail()) && !condition.getEmail().equals(existing.getEmail())) {
                checkUserUnique(null, condition.getEmail(), null, existing.getId());
                existing.setEmail(condition.getEmail());
            }
            if (notBlank(condition.getPhone()) && !condition.getPhone().equals(existing.getPhone())) {
                checkUserUnique(null, null, condition.getPhone(), existing.getId());
                existing.setPhone(condition.getPhone());
            }
            if (notBlank(condition.getSex())) {
                existing.setGender(condition.getSex());
            }
            if (notBlank(condition.getRole())) {
                existing.setRole(condition.getRole());
            }
            if (notBlank(condition.getAuthorization())) {
                existing.setAuthorization(condition.getAuthorization());
            }
            if (notBlank(condition.getPassword())) {
                existing.setPassword(passwordEncoder.encode(condition.getPassword()));
            }
        }

        existing.setUpdateId(admin.getId());
        existing.setUpdateName(admin.getUsername());
        existing.setUpdateTime(new Date());
        userRepository.updateById(existing);
        sanitizeUser(existing);
        return Result.build(existing);
    }

    @DeleteMapping("/{userId}")
    @ApiOperation("删除用户")
    public Result<?> deleteUser(@PathVariable Long userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "用户不存在");
        }
        userRepository.removeById(userId);
        return Result.build();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除用户")
    public Result<?> deleteUsers(@RequestBody java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "用户ID不能为空");
        }
        userRepository.removeByIds(userIds);
        return Result.build();
    }

    @PutMapping("/batch/role")
    @ApiOperation("批量更新用户角色")
    public Result<?> updateUserRoles(
            @RequestBody java.util.List<Long> userIds,
            @RequestParam String role,
            @RequestParam(required = false) String authorization) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "用户ID不能为空");
        }
        if (!notBlank(role)) {
            throw new BusinessException(Status.PARAM_ERROR, "角色不能为空");
        }
        User admin = getCurrentUser();
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.in("id", userIds)
                .set("role", role)
                .set("authorization", notBlank(authorization) ? authorization : role)
                .set("update_id", admin.getId())
                .set("update_name", admin.getUsername())
                .set("update_time", new Date());
        userRepository.update(wrapper);
        return Result.build();
    }

    private void checkUserUnique(String userName, String email, String phone, Long ignoreUserId) {
        if (notBlank(userName) && existsAnotherUser(userName, ignoreUserId)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "用户名已存在");
        }
        if (notBlank(email) && existsAnotherUser(email, ignoreUserId)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "邮箱已被注册");
        }
        if (notBlank(phone) && existsAnotherUser(phone, ignoreUserId)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "手机号已被注册");
        }
    }

    private boolean existsAnotherUser(String value, Long ignoreUserId) {
        User found = userRepository.findByUsername(value);
        if (found == null) {
            return false;
        }
        if (ignoreUserId == null) {
            return true;
        }
        return !ignoreUserId.equals(found.getId());
    }

    private void sanitizeUser(User user) {
        if (user != null) {
            user.setPassword(null);
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
