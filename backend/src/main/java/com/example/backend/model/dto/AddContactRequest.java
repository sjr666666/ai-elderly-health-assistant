package com.example.backend.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 添加紧急联系人请求DTO
 */
@Data
public class AddContactRequest {

    /**
     * 所属老人ID
     */
    @NotNull(message = "老人ID不能为空")
    private Long elderId;

    /**
     * 联系人姓名
     */
    @NotBlank(message = "联系人姓名不能为空")
    private String name;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    /**
     * 邮箱（可选）
     */
    private String email;

    /**
     * 关系（如子女、护工等）
     */
    private String relationship;
}
