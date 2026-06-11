package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 监护关系绑定请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianRelationRequest {

    /**
     * 监护人ID
     */
    private Long guardianId;

    /**
     * 老人ID（与elderUsername二选一）
     */
    private Long elderId;

    /**
     * 老人用户名（与elderId二选一）
     */
    private String elderUsername;

    /**
     * 关系类型
     */
    private String relationType;
}
