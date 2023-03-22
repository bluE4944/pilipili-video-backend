package com.pilipili.utils;

/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
public enum Status {
    /**
     * 成功
     */
    OK(200,"请求成功"),
    /**
     * 系统异常
     */
    SYSTEM_ERROR(101,"系统异常"),
    /**
     * 系统异常
     */
    DATA_NOT_FOUNT(102,"找不到数据"),
    /**
     * SQL语句异常
     */
    SQL_ERROR(109,"SQL语句异常");


    /**
     * 状态码
     */
    private final Integer code;
    /**
     * 消息
     */
    private final String message;

    Status(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Status{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
