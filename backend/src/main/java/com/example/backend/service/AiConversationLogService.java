package com.example.backend.service;

import com.example.backend.model.entity.AiConversationLog;

import java.util.List;

/**
 * AI对话记录服务接口
 */
public interface AiConversationLogService {

    /**
     * 保存对话记录
     *
     * @param userId    用户ID
     * @param queryType 查询类型
     * @param userInput 用户输入
     * @param aiOutput  AI输出
     * @param safetyPassed 是否通过安全检查
     * @return 保存的对话记录
     */
    AiConversationLog saveLog(Long userId, String queryType, String userInput, String aiOutput, boolean safetyPassed);

    /**
     * 根据用户ID查询对话历史
     *
     * @param userId 用户ID
     * @param limit  查询数量限制
     * @return 对话记录列表
     */
    List<AiConversationLog> getHistoryByUserId(Long userId, Integer limit);

    /**
     * 根据ID查询对话记录
     *
     * @param id 记录ID
     * @return 对话记录
     */
    AiConversationLog getLogById(Long id);

    /**
     * 删除单条对话记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     */
    boolean deleteLogById(Long id);

    /**
     * 清空用户所有对话记录
     *
     * @param userId 用户ID
     * @return 是否清空成功
     */
    boolean clearHistoryByUserId(Long userId);

    /**
     * 获取用户对话记录数量
     *
     * @param userId 用户ID
     * @return 记录数量
     */
    int countByUserId(Long userId);
}