package com.example.backend.common;

import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户活跃时间拦截器
 * 拦截关键请求，自动更新 last_active_time 字段
 * 5分钟内同一用户不重复更新，避免频繁写库
 * 从 SecurityContext 获取用户ID（数据库主键）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveTimeInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    /** 限频间隔（分钟） */
    private static final int UPDATE_INTERVAL_MINUTES = 5;

    /** 记录每个用户上次更新活跃时间的时间戳 */
    private final ConcurrentHashMap<Long, LocalDateTime> lastUpdateTimeMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return true;
        }

        // 检查是否需要更新（限频）
        LocalDateTime now = LocalDateTime.now();

        try {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                return true;
            }

            Long limitKey = user.getId();
            LocalDateTime lastUpdate = lastUpdateTimeMap.get(limitKey);
            if (lastUpdate != null && java.time.Duration.between(lastUpdate, now).toMinutes() < UPDATE_INTERVAL_MINUTES) {
                return true;
            }

            user.setLastActiveTime(now);
            userMapper.updateById(user);
            lastUpdateTimeMap.put(limitKey, now);
            log.debug("更新用户活跃时间 - id: {}, userId: {}, role: {}", user.getId(), user.getUserId(), user.getRole());
        } catch (Exception e) {
            log.warn("更新活跃时间失败 - error: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 从 SecurityContext 获取当前认证用户的ID（数据库主键）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
}
