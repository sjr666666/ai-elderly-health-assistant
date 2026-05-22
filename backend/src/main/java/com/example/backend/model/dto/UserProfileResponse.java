package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户档案响应DTO
 * 用于返回用户的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    /**
     * 用户ID（String类型，避免JavaScript精度丢失）
     */
    private String userId;

    /**
     * 真实姓名/称呼
     */
    private String realName;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 过敏史
     */
    private String allergyHistory;

    /**
     * 慢性病史
     */
    private String chronicDiseases;

    /**
     * 角色（elder/family）
     */
    private String role;
}
