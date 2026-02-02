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
    SQL_ERROR(109,"SQL语句异常"),
    /**
     * 参数错误
     */
    PARAM_ERROR(103,"参数错误"),
    /**
     * 未授权
     */
    UNAUTHORIZED(401,"未授权"),
    /**
     * 禁止访问
     */
    FORBIDDEN(403,"禁止访问"),
    /**
     * 资源不存在
     */
    NOT_FOUND(404,"资源不存在"),
    /**
     * 业务异常
     */
    BUSINESS_ERROR(400,"业务异常"),
    /**
     * 文件上传失败
     */
    UPLOAD_ERROR(201,"文件上传失败"),
    /**
     * 文件格式不支持
     */
    FILE_FORMAT_ERROR(202,"文件格式不支持"),
    /**
     * 文件大小超限
     */
    FILE_SIZE_ERROR(203,"文件大小超限");


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
