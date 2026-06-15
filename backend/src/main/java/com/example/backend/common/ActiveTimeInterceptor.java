package com.example.backend.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 老人端活跃时间拦截器
 * 拦截老人端的关键请求，自动更新 last_active_time 字段
 * 5分钟内同一用户不重复更新，避免频繁写库
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
        Long userId = extractUserId(request);
        if (userId == null) {
            return true;
        }

        // 检查是否需要更新（限频）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = lastUpdateTimeMap.get(userId);
        if (lastUpdate != null && java.time.Duration.between(lastUpdate, now).toMinutes() < UPDATE_INTERVAL_MINUTES) {
            return true;
        }

        // 查询用户并更新活跃时间（前端传的userId是user_id字段，不是自增主键id）
        try {
            LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
            query.eq(SysUser::getUserId, userId);
            SysUser user = userMapper.selectOne(query);
            if (user != null && "elder".equals(user.getRole())) {
                user.setLastActiveTime(now);
                userMapper.updateById(user);
                lastUpdateTimeMap.put(userId, now);
                log.debug("更新老人活跃时间 - userId: {}", userId);
            }
        } catch (Exception e) {
            log.warn("更新活跃时间失败 - userId: {}, error: {}", userId, e.getMessage());
        }

        return true;
    }

    /**
     * 从请求中提取用户ID
     * 优先从请求头获取，其次从请求参数获取
     */
    private Long extractUserId(HttpServletRequest request) {
        // 从请求头获取
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            // 从请求参数获取
            userIdStr = request.getParameter("userId");
        }
        if (userIdStr == null || userIdStr.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
