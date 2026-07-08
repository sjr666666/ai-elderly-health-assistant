package com.example.backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 区分业务异常（BusinessException）和系统异常（Exception），
 * 前者返回业务定义的状态码与消息，后者统一返回 500 并记录日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 —— 返回异常内携带的 code + message，前端可直接展示。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常 - code: {}, message: {}", ex.getCode(), ex.getMessage());
        return ResponseResult.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 系统异常 —— 统一返回 500，不将内部细节暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseResult.fail(ResponseCode.INTERNAL_ERROR);
    }
}
