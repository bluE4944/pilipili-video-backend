package com.pilipili.entity.out;

import com.pilipili.entity.User;
import lombok.Data;

/**
 * 登录结果
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
public class LoginResult {
    /**
     * Token
     */
    private String token;

    /**
     * Token类型
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private User user;

    public LoginResult(String token, Long expiresIn, User user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
