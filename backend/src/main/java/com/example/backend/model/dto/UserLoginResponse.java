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

    private String userId;  // 改为 String 避免 JavaScript 精度丢失
    private String username;
    private String realName;
    private Integer age;
    private String allergyHistory;
    private String chronicDiseases;
    private String role;
}
