package com.example.backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/** Keeps internal exception details out of public API responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseResult<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(toHttpStatus(exception.getCode()))
                .body(ResponseResult.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ResponseResult.fail(ResponseCode.VALIDATION_ERROR.getCode(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseResult<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(ResponseResult.fail(ResponseCode.VALIDATION_ERROR.getCode(), "请求参数校验失败"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(ResponseResult.fail(ResponseCode.PARAM_ERROR.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseResult.fail(ResponseCode.INTERNAL_ERROR));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private HttpStatus toHttpStatus(int code) {
        try {
            return HttpStatus.valueOf(code);
        } catch (IllegalArgumentException ignored) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
