package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    private Long id;  // 数据库主键ID，用于紧急联系人外键关联
    private String userId;  // 雪花算法ID（String类型，避免 JavaScript 精度丢失）
    private String username;
    private String realName;
    private Integer age;
    private String allergyHistory;
    private String chronicDiseases;
    private String role;
}
