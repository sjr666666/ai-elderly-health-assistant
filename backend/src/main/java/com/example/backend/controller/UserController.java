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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * 获取当前认证用户的ID（数据库主键）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    @PostMapping("/register")
    public ResponseResult<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        try {
            UserRegisterResponse response = userService.register(request);
            return ResponseResult.success("创建成功", response);
        } catch (Exception e) {
            // 捕获业务异常，返回友好的错误信息
            return ResponseResult.fail(e.getMessage());
        }
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
    public ResponseResult<UserProfileResponse> getProfile() {
        try {
            Long userId = getCurrentUserId();
            UserProfileResponse profile = userService.getUserProfile(userId);
            return ResponseResult.success("success", profile);
        } catch (Exception e) {
            return ResponseResult.fail("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     * PUT /api/v1/user/profile
     */
    @PutMapping("/profile")
    public ResponseResult<Void> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        try {
            Long userId = getCurrentUserId();
            userService.updateUserProfile(userId, request);
            return ResponseResult.success("更新成功", null);
        } catch (Exception e) {
            return ResponseResult.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     * PUT /api/v1/user/password
     */
    @PutMapping("/password")
    public ResponseResult<Void> changePassword(@RequestBody java.util.Map<String, String> body) {
        try {
            Long userId = getCurrentUserId();
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return ResponseResult.fail("旧密码和新密码不能为空");
            }
            userService.changePassword(userId, oldPassword, newPassword);
            return ResponseResult.success("密码修改成功", null);
        } catch (Exception e) {
            return ResponseResult.fail(e.getMessage());
        }
    }
}