package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
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
}