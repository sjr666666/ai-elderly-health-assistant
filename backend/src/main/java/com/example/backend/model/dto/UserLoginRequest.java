package com.example.backend.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 * 用于接收前端传递的登录参数
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
public class UserLoginRequest {

    /**
     * 登录名
     * 必填，不能为空
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     * 必填，不能为空
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}