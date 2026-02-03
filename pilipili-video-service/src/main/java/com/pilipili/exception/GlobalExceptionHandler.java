package com.pilipili.exception;

import com.pilipili.entity.out.Result;
import com.pilipili.utils.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 全局异常处理器
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return buildResponse(e.getStatus(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<Result<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return buildResponse(Status.PARAM_ERROR, message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler({BindException.class})
    public ResponseEntity<Result<?>> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.warn("参数绑定异常: {}", message);
        return buildResponse(Status.PARAM_ERROR, message);
    }

    /**
     * 处理约束校验异常
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Result<?>> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder message = new StringBuilder();
        for (ConstraintViolation<?> violation : violations) {
            message.append(violation.getMessage()).append("; ");
        }
        log.warn("约束校验异常: {}", message);
        return buildResponse(Status.PARAM_ERROR, message.toString());
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<?>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication error: {}", e.getMessage());
        return buildResponse(Status.UNAUTHORIZED, "Authentication failed, please login");
    }

    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return buildResponse(Status.FORBIDDEN, "Access denied");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("System error", e);
        return buildResponse(Status.SYSTEM_ERROR, "System error, please contact admin");
    }

    private ResponseEntity<Result<?>> buildResponse(Status status, String message) {
        return new ResponseEntity<>(Result.build(status, message), toHttpStatus(status));
    }

    private HttpStatus toHttpStatus(Status status) {
        if (status == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        switch (status) {
            case UNAUTHORIZED:
                return HttpStatus.UNAUTHORIZED;
            case FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            case NOT_FOUND:
            case DATA_NOT_FOUNT:
                return HttpStatus.NOT_FOUND;
            case PARAM_ERROR:
            case BUSINESS_ERROR:
            case FILE_FORMAT_ERROR:
            case FILE_SIZE_ERROR:
                return HttpStatus.BAD_REQUEST;
            case UPLOAD_ERROR:
            case SQL_ERROR:
            case SYSTEM_ERROR:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case OK:
                return HttpStatus.OK;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
