package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加紧急联系人响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddContactResponse {

    /**
     * 新增的联系人ID
     */
    private Long contactId;
}
