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

    private Long userId;
    private String username;
    private String realName;
    private Integer age;
    private String allergyHistory;
    private String chronicDiseases;
    private String role;
}
