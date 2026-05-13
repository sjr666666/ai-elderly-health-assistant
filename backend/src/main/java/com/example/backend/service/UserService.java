package com.example.backend.service;

import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserProfileResponse;
import com.example.backend.model.dto.UserProfileUpdateRequest;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;

public interface UserService {

    UserRegisterResponse register(UserRegisterRequest request);

    UserLoginResponse login(UserLoginRequest request);

    /**
     * 获取用户档案信息
     * @param userId 用户ID
     * @return 用户档案信息
     */
    UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户档案信息
     * @param userId 用户ID
     * @param request 更新请求
     */
    void updateUserProfile(Long userId, UserProfileUpdateRequest request);
}