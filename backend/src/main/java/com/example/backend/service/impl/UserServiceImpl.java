package com.example.backend.service.impl;

import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserRegisterResponse register(UserRegisterRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setAge(request.getAge());
        user.setAllergyHistory(request.getAllergyHistory());
        user.setChronicDiseases(request.getChronicDiseases());
        user.setRole(SysUser.Role.ELDER.getCode());

        userMapper.insert(user);

        return UserRegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(request.getPassword())
                .build();
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return null;
        }

        return UserLoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .age(user.getAge())
                .allergyHistory(user.getAllergyHistory())
                .chronicDiseases(user.getChronicDiseases())
                .role(user.getRole())
                .build();
    }
}