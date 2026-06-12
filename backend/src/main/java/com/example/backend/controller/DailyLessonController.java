package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DailyLessonDTO;
import com.example.backend.service.DailyLessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 获取今日科普（缓存优先）
     */
    @GetMapping("/today")
    public ResponseResult<DailyLessonDTO> getTodayLesson(@RequestParam Long userId) {
        try {
            DailyLessonDTO lesson = dailyLessonService.getTodayLesson(userId);
            return ResponseResult.success(lesson);
        } catch (Exception e) {
            logger.error("获取今日科普失败", e);
            return ResponseResult.fail("获取今日科普失败: " + e.getMessage());
        }
    }

    /**
     * 强制重新生成今日科普（换一篇）
     */
    @PostMapping("/regenerate")
    public ResponseResult<DailyLessonDTO> regenerateTodayLesson(@RequestParam Long userId) {
        try {
            DailyLessonDTO lesson = dailyLessonService.regenerateTodayLesson(userId);
            return ResponseResult.success(lesson);
        } catch (Exception e) {
            logger.error("重新生成今日科普失败", e);
            return ResponseResult.fail("重新生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取科普历史记录
     */
    @GetMapping("/history")
    public ResponseResult<List<DailyLessonDTO>> getHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<DailyLessonDTO> history = dailyLessonService.getLessonHistory(userId, page, size);
            return ResponseResult.success(history);
        } catch (Exception e) {
            logger.error("获取科普历史失败", e);
            return ResponseResult.fail("获取历史记录失败: " + e.getMessage());
        }
    }
}
