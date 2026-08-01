package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    private String gender;
    private BigDecimal height;
    private BigDecimal weight;
    private String allergyHistory;
    private String chronicDiseases;
    private String kidneyFunction;
    private String liverFunction;
    private Integer isPregnant;
    private Integer isBreastfeeding;
    private Integer isSmoking;
    private Integer isDrinking;
    private String role;
    private String token;  // JWT令牌，用于身份认证
    private String refreshToken;
}
