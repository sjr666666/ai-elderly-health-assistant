package com.example.backend.model.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9_\u4e00-\u9fa5]+$", message = "用户名只能包含字母、数字、下划线或中文")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    private String realName;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
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

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(elder|family)$", message = "角色只能是elder或family")
    private String role;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
