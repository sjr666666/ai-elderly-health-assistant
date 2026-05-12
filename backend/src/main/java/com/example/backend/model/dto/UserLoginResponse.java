package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应DTO
 * 用于返回给前端的登录结果
 * 注意：不包含密码等敏感信息
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色
     * elder（老人）/ family（家属）
     */
    private String role;

    /**
     * 访问令牌
     */
    private String token;
}