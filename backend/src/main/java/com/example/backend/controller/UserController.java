package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.config.JwtUtils;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserProfileResponse;
import com.example.backend.model.dto.UserProfileUpdateRequest;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;
import com.example.backend.service.UserService;
import com.example.backend.service.RefreshTokenService;
import com.example.backend.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;

    @Value("${jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Autowired
    public UserController(UserService userService, RefreshTokenService refreshTokenService,
                          TokenBlacklistService tokenBlacklistService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtils = jwtUtils;
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
        UserRegisterResponse response = userService.register(request);
        return ResponseResult.success("创建成功", response);
    }

    @PostMapping("/login")
    public ResponseResult<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request,
                                                   HttpServletResponse httpResponse) {
        UserLoginResponse response = userService.login(request);
        if (response == null) {
            return ResponseResult.fail("用户名或密码错误");
        }
        setRefreshCookie(httpResponse, response.getRefreshToken());
        response.setRefreshToken(null);
        return ResponseResult.success("登录成功", response);
    }

    @PostMapping("/refresh")
    public ResponseResult<Map<String, String>> refresh(HttpServletRequest request,
                                                       HttpServletResponse response) {
        Map<String, String> tokens = refreshTokenService.consumeAndRotate(readRefreshCookie(request));
        setRefreshCookie(response, tokens.remove("refreshToken"));
        return ResponseResult.success("刷新成功", tokens);
    }

    @PostMapping("/logout")
    public ResponseResult<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 撤销 refresh token（服务端 session 删除）
        refreshTokenService.revoke(readRefreshCookie(request));

        // 2. 将当前 access token 加入黑名单，使其立即失效
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String accessToken = bearer.substring(7);
            if (jwtUtils.validateToken(accessToken)) {
                tokenBlacklistService.blacklist(jwtUtils.getJtiFromToken(accessToken),
                        Duration.ofMillis(jwtUtils.getRemainingMillis(accessToken)));
            }
        }

        // 3. 清除 refresh cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Lax").path("/")
                .maxAge(Duration.ZERO).build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseResult.success("退出成功", null);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Lax").path("/")
                .maxAge(Duration.ofDays(30)).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 获取当前用户信息
     * GET /api/v1/user/profile
     */
    @GetMapping("/profile")
    public ResponseResult<UserProfileResponse> getProfile() {
        Long userId = getCurrentUserId();
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseResult.success("success", profile);
    }

    /**
     * 更新用户信息
     * PUT /api/v1/user/profile
     */
    @PutMapping("/profile")
    public ResponseResult<Void> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        Long userId = getCurrentUserId();
        userService.updateUserProfile(userId, request);
        return ResponseResult.success("更新成功", null);
    }

    /**
     * 修改密码
     * PUT /api/v1/user/password
     */
    @PutMapping("/password")
    public ResponseResult<Void> changePassword(@RequestBody java.util.Map<String, String> body) {
        Long userId = getCurrentUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseResult.success("密码修改成功", null);
    }
}
