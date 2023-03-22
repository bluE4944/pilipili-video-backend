package com.pilipili.entity.out;

import com.pilipili.utils.Status;

/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:20
 */
public class Result<T> {
    /**
     * 状态码
     */
    private Integer status;
    /**
     * 错误的状态信息
     */
    private String message;
    /**
     * 数据
     */
    private T body;


    public Integer getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public T getBody() {
        return body;
    }
    /**
     * 构造器(私有化)，这里写了3个构造器。根据实际发开需要添加即可
     */
    private Result(Integer status,String message,T body){
        this.status=status;
        this.message=message;
        this.body=body;
    }
    private Result(Integer status,String message){
        this.status=status;
        this.message=message;
    }
    private Result(String message){
        this.message=message;
    }

    //下面就是根据需要返回不同参数格式的方法
    /**
     * 返回《状态码》《状态信息》《数据》
     * 状态码来自--->>枚举
     * 状态信息来自--->>开发人员
     * 数据来自--->>开发人员
     */
    public static <T> Result<T> build(Status status, String message, T body){
        return new Result<T>(status.getCode(),message,body);

    }

    /**
     * 返回《状态码》《状态信息》《状态信息》
     * 状态码来自--->>枚举
     * 状态信息来自--->>开发人员
     */
    public static <T> Result<T> build(Status status,String message){
        return new Result<T>(status.getCode(),message);
    }
    /**
     * 返回《状态码》《状态信息》《数据》
     * 状态码来自--->>枚举
     * 状态信息来自--->>枚举
     * 数据来自--->>开发人员
     */
    public static <T> Result<T> build(Status status,T body){
        return new Result<T>(status.getCode(),status.getMessage(),body);
    }
    /**
     * 返回《状态码》《状态信息》《数据》
     * 状态码来自--->>枚举
     * 状态信息来自--->>枚举
     * 数据来自--->>开发人员
     */
    public static <T> Result<T> build(Status status){
        return new Result<T>(status.getCode(),status.getMessage());
    }

    /**
     * 构建返回内容
     * @param body 内容
     * @return Result
     */
    public static <T> Result<T> build(T body){
        return new Result<T>(Status.OK.getCode(),Status.OK.getMessage(),body);
    }

    /**
     * 构建返回内容
     * @return Result
     */
    public static <T> Result<T> build(){
        return new Result<T>(Status.OK.getCode(),Status.OK.getMessage());
    }

    public boolean isSuccess(){
        return this.status.compareTo(Status.OK.getCode()) == 0;
    }
}
