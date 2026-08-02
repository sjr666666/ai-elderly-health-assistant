package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DailyLessonDTO;
import com.example.backend.service.DailyLessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 今日一课 - 慢病科普控制器
 *
 * @author backend
 * @since 1.1.0
 */
@RestController
@RequestMapping("/api/v1/daily-lesson")
@CrossOrigin(origins = "*")
public class DailyLessonController {

    private static final Logger logger = LoggerFactory.getLogger(DailyLessonController.class);

    @Autowired
    private DailyLessonService dailyLessonService;

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

    /**
     * 获取今日科普（缓存优先）
     */
    @GetMapping("/today")
    public ResponseResult<DailyLessonDTO> getTodayLesson() {
        Long userId = getCurrentUserId();
        DailyLessonDTO lesson = dailyLessonService.getTodayLesson(userId);
        return ResponseResult.success(lesson);
    }

    /**
     * 强制重新生成今日科普（换一篇）
     */
    @PostMapping("/regenerate")
    public ResponseResult<DailyLessonDTO> regenerateTodayLesson() {
        Long userId = getCurrentUserId();
        DailyLessonDTO lesson = dailyLessonService.regenerateTodayLesson(userId);
        return ResponseResult.success(lesson);
    }

    /**
     * 获取科普历史记录
     */
    @GetMapping("/history")
    public ResponseResult<List<DailyLessonDTO>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        List<DailyLessonDTO> history = dailyLessonService.getLessonHistory(userId, page, size);
        return ResponseResult.success(history);
    }
}

