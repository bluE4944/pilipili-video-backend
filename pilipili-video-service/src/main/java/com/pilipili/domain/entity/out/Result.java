package com.pilipili.domain.entity.out;

import com.pilipili.utils.ResultStatus;

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
    public static <T> Result<T> build(ResultStatus resultStatus, String message, T body){
        return new Result<T>(resultStatus.getCode(),message,body);

    }

    /**
     * 返回《状态码》《状态信息》《状态信息》
     * 状态码来自--->>枚举
     * 状态信息来自--->>开发人员
     */
    public static <T> Result<T> build(ResultStatus resultStatus, String message){
        return new Result<T>(resultStatus.getCode(),message);
    }
    /**
     * 返回《状态码》《状态信息》《数据》
     * 状态码来自--->>枚举
     * 状态信息来自--->>枚举
     * 数据来自--->>开发人员
     */
    public static <T> Result<T> build(ResultStatus resultStatus, T body){
        return new Result<T>(resultStatus.getCode(), resultStatus.getMessage(),body);
    }
    /**
     * 返回《状态码》《状态信息》《数据》
     * 状态码来自--->>枚举
     * 状态信息来自--->>枚举
     * 数据来自--->>开发人员
     */
    public static <T> Result<T> build(ResultStatus resultStatus){
        return new Result<T>(resultStatus.getCode(), resultStatus.getMessage());
    }

    /**
     * 构建返回内容
     * @param body 内容
     * @return Result
     */
    public static <T> Result<T> build(T body){
        return new Result<T>(com.pilipili.utils.ResultStatus.OK.getCode(), com.pilipili.utils.ResultStatus.OK.getMessage(),body);
    }

    /**
     * 构建返回内容
     * @return Result
     */
    public static <T> Result<T> build(){
        return new Result<T>(com.pilipili.utils.ResultStatus.OK.getCode(), com.pilipili.utils.ResultStatus.OK.getMessage());
    }

    public boolean isSuccess(){
        return this.status.compareTo(com.pilipili.utils.ResultStatus.OK.getCode()) == 0;
    }
}
