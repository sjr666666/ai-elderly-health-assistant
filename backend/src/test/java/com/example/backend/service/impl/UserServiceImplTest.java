package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.config.JwtUtils;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.ElderNotificationService;
import com.example.backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 登录服务单元测试
 * 核心业务规则:用户不存在与密码错误统一返回 null(防账号枚举)
 */
class UserServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private RefreshTokenService refreshTokenService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtils = mock(JwtUtils.class);
        refreshTokenService = mock(RefreshTokenService.class);
        ElderNotificationService elderNotificationService = mock(ElderNotificationService.class);
        GuardianElderRelationMapper relationMapper = mock(GuardianElderRelationMapper.class);

        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtils,
                elderNotificationService, relationMapper, refreshTokenService);
    }

    private SysUser elderUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserId(10001L);
        user.setUsername("laowang");
        user.setPassword("$2a$10$encodedhash");
        user.setRealName("王阿姨");
        user.setRole("elder");
        return user;
    }

    @Test
    void login_userNotFound_returnsNull() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("ghost");
        request.setPassword("123456");
        assertNull(userService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_wrongPassword_returnsNull() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(elderUser());
        when(passwordEncoder.matches("wrong-password", "$2a$10$encodedhash")).thenReturn(false);
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("laowang");
        request.setPassword("wrong-password");
        assertNull(userService.login(request));
        // 防枚举:与用户不存在返回一致(null),且不生成 token
        verify(jwtUtils, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_correctCredentials_returnsResponseWithToken() {
        SysUser user = elderUser();
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("123456", user.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(1L, "laowang", "elder")).thenReturn("jwt-token");

        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("laowang");
        request.setPassword("123456");

        UserLoginResponse response = userService.login(request);
        assertNotNull(response);
        assertEquals("laowang", response.getUsername());
        assertEquals("10001", response.getUserId());
        assertEquals("elder", response.getRole());
        verify(refreshTokenService).issue(1L, "laowang", "elder");
    }
}
