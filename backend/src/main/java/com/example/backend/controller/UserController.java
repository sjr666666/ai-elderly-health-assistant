package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserProfileResponse;
import com.example.backend.model.dto.UserProfileUpdateRequest;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseResult<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserRegisterResponse response = userService.register(request);
        return ResponseResult.success("创建成功", response);
    }

    @PostMapping("/login")
    public ResponseResult<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);
        if (response == null) {
            return ResponseResult.fail("用户名或密码错误");
        }
        return ResponseResult.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     * GET /api/v1/user/profile
     */
    @GetMapping("/profile")
    public ResponseResult<UserProfileResponse> getProfile(@RequestParam String userId) {
        try {
            // 将 String 转换为 Long，避免精度丢失
            Long userIdLong = Long.parseLong(userId);
            UserProfileResponse profile = userService.getUserProfile(userIdLong);
            return ResponseResult.success("success", profile);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     * PUT /api/v1/user/profile
     */
    @PutMapping("/profile")
    public ResponseResult<Void> updateProfile(
            @RequestParam String userId,
            @RequestBody UserProfileUpdateRequest request) {
        try {
            // 将 String 转换为 Long，避免精度丢失
            Long userIdLong = Long.parseLong(userId);
            userService.updateUserProfile(userIdLong, request);
            return ResponseResult.success("更新成功", null);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("更新失败: " + e.getMessage());
        }
    }
}