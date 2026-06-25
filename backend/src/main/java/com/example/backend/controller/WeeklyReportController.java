package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.WeeklyReportRequest;
import com.example.backend.model.dto.WeeklyReportResponse;
import com.example.backend.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 用药周报控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/weekly-report")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    /**
     * 生成用药周报
     * 
     * @param userId 用户ID
     * @param startDate 起始日期（可选，默认7天前）
     * @param endDate 结束日期（可选，默认今天）
     * @return 用药周报
     */
    @PostMapping("/generate")
    public ResponseResult<WeeklyReportResponse> generateWeeklyReport(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            log.info("收到用药周报生成请求 - userId: {}, startDate: {}, endDate: {}", 
                    userId, startDate, endDate);

            // 构建请求对象
            WeeklyReportRequest request = WeeklyReportRequest.builder()
                    .userId(userId)
                    .startDate(startDate != null ? LocalDate.parse(startDate) : null)
                    .endDate(endDate != null ? LocalDate.parse(endDate) : null)
                    .build();

            // 生成周报
            WeeklyReportResponse report = weeklyReportService.generateWeeklyReport(request);

            return ResponseResult.success("用药周报生成成功", report);

        } catch (Exception e) {
            log.error("用药周报生成失败: ", e);
            return ResponseResult.fail("用药周报生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近一周的用药周报（快捷接口）
     * 
     * @param userId 用户ID
     * @return 用药周报
     */
    @GetMapping("/latest")
    public ResponseResult<WeeklyReportResponse> getLatestWeeklyReport(@RequestParam Long userId) {
        try {
            log.info("获取最新用药周报 - userId: {}", userId);

            WeeklyReportRequest request = WeeklyReportRequest.builder()
                    .userId(userId)
                    .build();

            WeeklyReportResponse report = weeklyReportService.generateWeeklyReport(request);

            return ResponseResult.success("用药周报获取成功", report);

        } catch (Exception e) {
            log.error("获取用药周报失败: ", e);
            return ResponseResult.fail("获取用药周报失败: " + e.getMessage());
        }
    }
}
