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
    ROLE_USER("ROLE_USER"),
    /**
     * 管理员
     */
    ROLE_MANAGE("ROLE_MANAGE"),
    /**
     * admin
     */
    ROLE_ADMIN("ROLE_ADMIN");


    private final String code;

    Role(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
