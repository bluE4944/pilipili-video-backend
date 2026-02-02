package com.pilipili.exception;

import com.pilipili.utils.Status;
import lombok.Getter;

/**
 * 业务异常
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Status status;
    
    public BusinessException(Status status) {
        super(status.getMessage());
        this.status = status;
    }
    
    public BusinessException(Status status, String message) {
        super(message);
        this.status = status;
    }
    
    public BusinessException(String message) {
        super(message);
        this.status = Status.BUSINESS_ERROR;
    }
}
