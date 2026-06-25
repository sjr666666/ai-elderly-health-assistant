package com.example.backend.service;

import com.example.backend.model.dto.WeeklyReportRequest;
import com.example.backend.model.dto.WeeklyReportResponse;

/**
 * 用药周报服务接口
 */
public interface WeeklyReportService {
    
    /**
     * 生成用药周报
     * 
     * @param request 周报请求参数
     * @return 用药周报响应
     */
    WeeklyReportResponse generateWeeklyReport(WeeklyReportRequest request);
}
