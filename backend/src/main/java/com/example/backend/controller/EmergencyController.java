package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.AddContactRequest;
import com.example.backend.model.dto.AddContactResponse;
import com.example.backend.model.entity.AiConversationLog;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.service.AiConversationLogService;
import com.example.backend.service.EmergencyContactService;
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
    private final EmergencyContactService emergencyContactService;

    public EmergencyController(AiEmergencyServiceImpl aiEmergencyService,
                               AiConversationLogService conversationLogService,
                               EmergencyContactService emergencyContactService) {
        this.aiEmergencyService = aiEmergencyService;
        this.conversationLogService = conversationLogService;
        this.emergencyContactService = emergencyContactService;
    }

    /**
     * 请求体DTO
     */
    public static class EmergencyRequest {
        private Long userId;
        private String question;
        private Boolean isEmergency;
        private String category;
        private List<Map<String, String>> history; // 对话历史，用于实现记忆功能

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public Boolean getIsEmergency() { return isEmergency; }
        public void setIsEmergency(Boolean isEmergency) { this.isEmergency = isEmergency; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<Map<String, String>> getHistory() { return history; }
        public void setHistory(List<Map<String, String>> history) { this.history = history; }
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
        List<Map<String, String>> history = request.getHistory();
        
        logger.info("收到紧急咨询请求 - 用户ID: {}, 问题: {}, 紧急标识: {}, 历史记录数: {}", 
                userId, question, isEmergency, history != null ? history.size() : 0);

        // 如果未指定是否紧急，自动判断
        boolean emergencyFlag = isEmergency != null ? isEmergency 
                                                    : aiEmergencyService.isEmergencyQuestion(question);

        return aiEmergencyService.handleEmergencyQuestion(userId, question, emergencyFlag, history);
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

    /**
     * 根据老人ID获取紧急联系人
     *
     * @param elderId 老人ID
     * @return 紧急联系人信息
     */
    @GetMapping("/emergency-contact")
    public ResponseResult<EmergencyContact> getContactByElderId(@RequestParam("elderId") Long elderId) {
        logger.info("获取紧急联系人请求 - elderId: {}", elderId);
        EmergencyContact contact = emergencyContactService.getContactByElderId(elderId);
        return ResponseResult.success(contact);
    }

    /**
     * 获取所有紧急联系人列表
     * GET /api/v1/contacts
     *
     * @param elderId 老人ID
     * @return 紧急联系人列表
     */
    @GetMapping(value = "/v1/contacts", produces = "application/json;charset=UTF-8")
    public ResponseResult<List<EmergencyContact>> getContactsByElderId(@RequestParam("elderId") Long elderId) {
        logger.info("获取所有紧急联系人请求 - elderId: {}", elderId);
        List<EmergencyContact> contacts = emergencyContactService.getContactsByElderId(elderId);
        return ResponseResult.success(contacts);
    }

    /**
     * 更新紧急联系人信息
     *
     * @param contact 紧急联系人信息
     * @return 更新后的紧急联系人信息
     */
    @PutMapping("/emergency-contact")
    public ResponseResult<EmergencyContact> updateContact(@RequestBody EmergencyContact contact) {
        logger.info("更新紧急联系人请求 - id: {}, elderId: {}, isPrimary: {}", contact.getId(), contact.getElderId(), contact.getIsPrimary());
        EmergencyContact updatedContact = emergencyContactService.updateContact(contact);
        return ResponseResult.success(updatedContact);
    }

    /**
     * 添加联系人
     * POST /api/v1/contacts
     *
     * @param request 联系人信息
     * @return 添加结果
     */
    @PostMapping(value = "/v1/contacts", produces = "application/json;charset=UTF-8")
    public ResponseResult<AddContactResponse> addContact(@RequestBody AddContactRequest request) {
        logger.info("添加联系人请求 - name: {}, elderId: {}", request.getName(), request.getElderId());

        // 创建联系人实体
        EmergencyContact contact = new EmergencyContact();
        contact.setName(request.getName());
        contact.setElderId(request.getElderId());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setRelationship(request.getRelationship());
        
        // 检查该用户是否已经有主要联系人
        EmergencyContact existingPrimary = emergencyContactService.getContactByElderId(request.getElderId());
        if (existingPrimary == null) {
            // 如果还没有主要联系人，则新添加的联系人自动成为主要联系人
            contact.setIsPrimary(1);
            logger.info("这是第一个联系人，自动设置为主要联系人");
        } else {
            // 如果已有主要联系人，新添加的不是主要联系人
            contact.setIsPrimary(0);
            logger.info("已存在主要联系人，新联系人不作为主要联系人");
        }

        // 保存联系人
        EmergencyContact savedContact = emergencyContactService.saveContact(contact);

        // 构建响应
        AddContactResponse response = AddContactResponse.builder()
                .contactId(savedContact.getId())
                .build();

        return ResponseResult.success("添加成功", response);
    }

    /**
     * 删除联系人
     * DELETE /api/v1/contacts/{id}
     *
     * @param id 联系人ID
     * @return 删除结果
     */
    @DeleteMapping(value = "/v1/contacts/{id}", produces = "application/json;charset=UTF-8")
    public ResponseResult<String> deleteContact(@PathVariable("id") Long id) {
        logger.info("删除联系人请求 - id: {}", id);

        boolean success = emergencyContactService.deleteContact(id);

        if (success) {
            return ResponseResult.success("已删除", null);
        } else {
            return ResponseResult.fail("删除失败，联系人不存在");
        }
    }
}
