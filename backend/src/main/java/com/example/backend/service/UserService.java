package com.example.backend.service;

import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;

public interface UserService {

    UserRegisterResponse register(UserRegisterRequest request);

    UserLoginResponse login(UserLoginRequest request);
}