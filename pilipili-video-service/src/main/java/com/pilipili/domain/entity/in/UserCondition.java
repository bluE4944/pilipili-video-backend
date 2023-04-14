package com.pilipili.domain.entity.in;

import lombok.Data;

/**
 * @author Li
 * @version 1.0
 * @ClassName UserCondition
 * @Description
 * @since 2023/3/23 14:02
 */
@Data
public class UserCondition {
    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nikeName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 性别
     */
    private String sex;

    /**
     * 角色
     */
    private String role;

    /**
     * 授权
     */
    private String authorization;
}
