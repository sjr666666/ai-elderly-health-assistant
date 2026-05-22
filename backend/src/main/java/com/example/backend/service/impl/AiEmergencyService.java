package com.example.backend.service.impl;

import com.example.backend.common.ResponseResult;

import java.util.List;
import java.util.Map;

/**
 * AI紧急助手服务接口
 */
public interface AiEmergencyService {

    /**
     * 处理紧急问题咨询
     *
     * @param userId    用户ID
     * @param question  用户问题
     * @param isEmergency 是否紧急
     * @param history   对话历史，用于实现记忆功能
     * @return AI响应结果
     */
    ResponseResult<String> handleEmergencyQuestion(Long userId, String question, boolean isEmergency, List<Map<String, String>> history);

    /**
     * 获取离线模式下的基础应答
     *
     * @param question 用户问题
     * @return 离线应答内容
     */
    String getOfflineResponse(String question);

    /**
     * 判断问题是否为紧急情况
     *
     * @param question 用户问题
     * @return 是否紧急
     */
    boolean isEmergencyQuestion(String question);

    /**
     * 获取问题分类标签列表
     *
     * @return 分类标签列表
     */
    ResponseResult<?> getCategoryTags();
}