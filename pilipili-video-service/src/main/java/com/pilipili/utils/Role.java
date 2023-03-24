package com.pilipili.utils;


/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:50
 */
public enum Role {
    /**
     * 用户
     */
    ROLE_USER("user"),
    /**
     * 管理员
     */
    ROLE_MANAGE("manage"),
    /**
     * admin
     */
    ROLE_ADMIN("admin");


    private final String code;

    Role(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
