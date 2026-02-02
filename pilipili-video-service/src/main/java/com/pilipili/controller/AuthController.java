package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.in.LoginRequest;
import com.pilipili.entity.out.LoginResult;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.UserRepository;
import com.pilipili.service.UserService;
import com.pilipili.utils.JwtUtil;
import com.pilipili.utils.RSAUtil;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;

/**
 * 认证控制器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Api(value = "认证控制器", tags = "用户认证")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<LoginResult> login(@RequestBody LoginRequest loginRequest) {
        try {
            // RSA解密密码（如果前端加密了）
            String password = loginRequest.getPassword();
            try {
                password = RSAUtil.decrypt(URLDecoder.decode(password, "UTF-8"), RSAUtil.private_key);
            } catch (Exception e) {
                log.debug("密码未加密或解密失败，使用原密码");
            }

            // 查找用户
            User user = userRepository.findByUsername(loginRequest.getUsername());
            if (user == null) {
                throw new BusinessException(Status.UNAUTHORIZED, "用户名或密码错误");
            }

            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(Status.UNAUTHORIZED, "用户名或密码错误");
            }

            // 生成Token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            Long expiresIn = jwtUtil.getExpiration() / 1000; // 转换为秒

            // 清除敏感信息
            user.setPassword(null);

            LoginResult loginResult = new LoginResult(token, expiresIn, user);
            log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
            
            return Result.build(loginResult);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("登录失败", e);
            throw new BusinessException(Status.SYSTEM_ERROR, "登录失败：" + e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @ApiOperation("刷新Token")
    public Result<LoginResult> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new BusinessException(Status.UNAUTHORIZED, "Token无效");
        }

        String newToken = jwtUtil.refreshToken(token);
        if (newToken == null) {
            throw new BusinessException(Status.UNAUTHORIZED, "Token刷新失败");
        }

        Long expiresIn = jwtUtil.getExpiration() / 1000;
        LoginResult loginResult = new LoginResult(newToken, expiresIn, null);
        return Result.build(loginResult);
    }
}
