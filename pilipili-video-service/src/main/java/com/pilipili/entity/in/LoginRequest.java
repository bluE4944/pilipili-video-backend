package com.pilipili.entity.in;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录请求
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@ApiModel("登录请求")
public class LoginRequest {
    /**
     * 用户名（可以是用户名、邮箱或手机号）
     */
    @ApiModelProperty(value = "用户名（可以是用户名、邮箱或手机号）", required = true, example = "admin")
    private String username;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;
}
