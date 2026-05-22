package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.entity.AiConversationLog;
import com.example.backend.service.AiConversationLogService;
import com.example.backend.service.impl.AiEmergencyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI紧急助手控制器
 */
@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyController {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyController.class);

    private final AiEmergencyServiceImpl aiEmergencyService;
    private final AiConversationLogService conversationLogService;

    public EmergencyController(AiEmergencyServiceImpl aiEmergencyService,
                               AiConversationLogService conversationLogService) {
        this.aiEmergencyService = aiEmergencyService;
        this.conversationLogService = conversationLogService;
    }

    /**
     * 请求体DTO
     */
    public static class EmergencyRequest {
        private Long userId;
        private String question;
        private Boolean isEmergency;
        private String category;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public Boolean getIsEmergency() { return isEmergency; }
        public void setIsEmergency(Boolean isEmergency) { this.isEmergency = isEmergency; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    /**
     * 紧急咨询接口
     *
     * @param request 包含用户ID、问题、紧急标识的请求体
     * @return AI响应结果
     */
    @PostMapping("/ask")
    public ResponseResult<String> askEmergencyQuestion(@RequestBody EmergencyRequest request) {
        
        Long userId = request.getUserId();
        String question = request.getQuestion();
        Boolean isEmergency = request.getIsEmergency();
        
        logger.info("收到紧急咨询请求 - 用户ID: {}, 问题: {}, 紧急标识: {}", 
                userId, question, isEmergency);

        // 如果未指定是否紧急，自动判断
        boolean emergencyFlag = isEmergency != null ? isEmergency 
                                                    : aiEmergencyService.isEmergencyQuestion(question);

        return aiEmergencyService.handleEmergencyQuestion(userId, question, emergencyFlag);
    }

    /**
     * 获取对话历史
     *
     * @param userId 用户ID
     * @param limit  返回数量限制（可选，默认20）
     * @return 对话历史列表
     */
    @GetMapping("/history")
    public ResponseResult<List<AiConversationLog>> getConversationHistory(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        
        logger.info("查询对话历史 - 用户ID: {}, 限制: {}", userId, limit);
        
        List<AiConversationLog> history = conversationLogService.getHistoryByUserId(userId, limit);
        return ResponseResult.success(history);
    }

    /**
     * 清空对话历史
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public ResponseResult<String> clearConversationHistory(
            @RequestParam("userId") Long userId) {
        
        logger.info("清空对话历史 - 用户ID: {}", userId);
        
        boolean success = conversationLogService.clearHistoryByUserId(userId);
        if (success) {
            return ResponseResult.success("对话历史已清空");
        } else {
            return ResponseResult.fail("清空失败");
        }
    }

    /**
     * 获取问题分类标签
     *
     * @return 分类标签列表
     */
    @GetMapping("/categories")
    public ResponseResult<?> getCategoryTags() {
        return aiEmergencyService.getCategoryTags();
    }

    /**
     * 健康自检接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "AI紧急助手服务");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }
}
