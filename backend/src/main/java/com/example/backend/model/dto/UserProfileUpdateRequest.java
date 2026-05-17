package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户档案更新请求DTO
 * 用于更新用户档案信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    /**
     * 过敏史
     */
    private String allergyHistory;

    /**
     * 慢性病史
     */
    private String chronicDiseases;
}
