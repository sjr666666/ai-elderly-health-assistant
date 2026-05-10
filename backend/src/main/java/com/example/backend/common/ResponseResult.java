package com.example.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    public static <T> ResponseResult<T> success() {
        return ResponseResult.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .build();
    }

    public static <T> ResponseResult<T> success(T data) {
        return ResponseResult.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        return ResponseResult.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseResult<T> fail() {
        return ResponseResult.<T>builder()
                .code(ResponseCode.FAIL.getCode())
                .message(ResponseCode.FAIL.getMessage())
                .build();
    }

    public static <T> ResponseResult<T> fail(String message) {
        return ResponseResult.<T>builder()
                .code(ResponseCode.FAIL.getCode())
                .message(message)
                .build();
    }

    public static <T> ResponseResult<T> fail(int code, String message) {
        return ResponseResult.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> ResponseResult<T> fail(ResponseCode responseCode) {
        return ResponseResult.<T>builder()
                .code(responseCode.getCode())
                .message(responseCode.getMessage())
                .build();
    }

    public static <T> ResponseResult<T> of(ResponseCode responseCode, T data) {
        return ResponseResult.<T>builder()
                .code(responseCode.getCode())
                .message(responseCode.getMessage())
                .data(data)
                .build();
    }

    public static <T> ResponseResult<T> of(int code, String message, T data) {
        return ResponseResult.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}