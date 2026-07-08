package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.AddContactRequest;
import com.example.backend.model.dto.AddContactResponse;
import com.example.backend.model.entity.AiConversationLog;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.model.entity.EmergencyEvent;
import com.example.backend.model.entity.GuardianElderRelation;
import com.example.backend.model.entity.SysUser;
import com.example.backend.model.enums.EventType;
import com.example.backend.model.enums.RelationStatus;
import com.example.backend.model.enums.Severity;
import com.example.backend.service.AiConversationLogService;
import com.example.backend.service.EmergencyContactService;
import com.example.backend.service.EmergencyEventService;
import com.example.backend.service.SmsNotificationService;
import com.example.backend.service.impl.AiEmergencyServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final EmergencyEventService emergencyEventService;
    private final SmsNotificationService smsNotificationService;
    private final GuardianElderRelationMapper guardianElderRelationMapper;
    private final UserMapper userMapper;

    public EmergencyController(AiEmergencyServiceImpl aiEmergencyService,
                               AiConversationLogService conversationLogService,
                               EmergencyContactService emergencyContactService,
                               EmergencyEventService emergencyEventService,
                               SmsNotificationService smsNotificationService,
                               GuardianElderRelationMapper guardianElderRelationMapper,
                               UserMapper userMapper) {
        this.aiEmergencyService = aiEmergencyService;
        this.conversationLogService = conversationLogService;
        this.emergencyContactService = emergencyContactService;
        this.emergencyEventService = emergencyEventService;
        this.smsNotificationService = smsNotificationService;
        this.guardianElderRelationMapper = guardianElderRelationMapper;
        this.userMapper = userMapper;
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

        Long userId = getCurrentUserId();
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
     * 触发紧急模式
     * 老人开启紧急模式时调用，创建紧急事件并通知所有关联家属
     *
     * @return 操作结果
     */
    @PostMapping("/trigger")
    public ResponseResult<Map<String, Object>> triggerEmergencyMode() {
        Long elderId = getCurrentUserId();

        logger.info("老人触发紧急模式 - elderId: {}", elderId);

        // 查询老人信息
        SysUser elder = userMapper.selectById(elderId);
        String elderName = elder != null ? elder.getRealName() : "老人";

        // 创建紧急事件
        EmergencyEvent event = new EmergencyEvent();
        event.setElderId(elderId);
        event.setEventType(EventType.SOS.getCode());
        event.setSeverity(Severity.HIGH.getCode());
        event.setDescription(elderName + "开启了紧急求助模式");
        event.setEventTime(LocalDateTime.now());
        event.setIsResolved(0);
        emergencyEventService.createEvent(event);

        logger.info("紧急事件已创建 - eventId: {}", event.getId());

        // 查询所有关联家属
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        // 通知每位家属
        int notifyCount = 0;
        for (GuardianElderRelation relation : relations) {
            SysUser guardian = userMapper.selectById(relation.getGuardianId());
            String phone = guardian != null ? guardian.getPhone() : "";
            try {
                String message = String.format("【紧急求助】%s开启了紧急求助模式，请立即关注！", elderName);
                smsNotificationService.sendNotification(
                        relation.getGuardianId(), elderId, EventType.EMERGENCY_ALERT.getCode(), message, phone);
                notifyCount++;
            } catch (Exception e) {
                logger.error("通知家属失败 - guardianId: {}", relation.getGuardianId(), e);
            }
        }

        logger.info("紧急模式触发完成 - 通知家属数: {}", notifyCount);

        Map<String, Object> result = new HashMap<>();
        result.put("eventId", event.getId());
        result.put("notifiedGuardians", notifyCount);
        return ResponseResult.success(result);
    }

    /**
     * 获取对话历史
     *
     * @param limit  返回数量限制（可选，默认20）
     * @return 对话历史列表
     */
    @GetMapping("/history")
    public ResponseResult<List<AiConversationLog>> getConversationHistory(
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {

        Long userId = getCurrentUserId();
        logger.info("查询对话历史 - 用户ID: {}, 限制: {}", userId, limit);

        List<AiConversationLog> history = conversationLogService.getHistoryByUserId(userId, limit);
        return ResponseResult.success(history);
    }

    /**
     * 清空对话历史
     *
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public ResponseResult<String> clearConversationHistory() {

        Long userId = getCurrentUserId();
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
