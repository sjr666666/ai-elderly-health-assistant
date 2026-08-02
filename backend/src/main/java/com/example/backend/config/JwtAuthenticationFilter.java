package com.example.backend.config;

import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器 - 从请求头中提取并验证JWT令牌
 * 校验链：签名/过期 → 登出黑名单 → 用户仍存在（未被逻辑删除）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                String jti = jwtUtils.getJtiFromToken(jwt);

                // 1. 登出黑名单校验：已登出的 token 直接拒绝
                if (tokenBlacklistService.isBlacklisted(jti)) {
                    log.debug("JWT token is blacklisted, rejecting - jti: {}", jti);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. 用户状态校验：逻辑删除的用户其 token 立即失效
                Long userId = jwtUtils.getUserIdFromToken(jwt);
                SysUser user = userMapper.selectById(userId);
                if (user == null) {
                    log.debug("JWT user no longer exists, rejecting - userId: {}", userId);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtUtils.getUsernameFromToken(jwt);
                String role = jwtUtils.getRoleFromToken(jwt);

                // 创建认证对象
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT认证成功 - userId: {}, username: {}, role: {}", userId, username, role);
            }
        } catch (Exception e) {
            log.error("JWT认证过程中发生异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
