package com.example.backend.service.impl;

import com.example.backend.mapper.AiConversationLogMapper;
import com.example.backend.model.entity.AiConversationLog;
import com.example.backend.service.AiConversationLogService;
import com.example.backend.common.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话记录服务实现类
 */
@Service
public class AiConversationLogServiceImpl implements AiConversationLogService {

    private static final Logger logger = LoggerFactory.getLogger(AiConversationLogServiceImpl.class);

    private final AiConversationLogMapper conversationLogMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AiConversationLogServiceImpl(AiConversationLogMapper conversationLogMapper,
                                        SnowflakeIdGenerator idGenerator) {
        this.conversationLogMapper = conversationLogMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public AiConversationLog saveLog(Long userId, String queryType, String userInput, String aiOutput, boolean safetyPassed) {
        try {
            AiConversationLog log = new AiConversationLog();
            log.setId(idGenerator.nextId());
            log.setUserId(userId);
            log.setQueryType(queryType);
            log.setUserInput(userInput);
            log.setAiOutput(aiOutput);
            log.setSafetyCheckPassed(safetyPassed);
            log.setCreatedAt(LocalDateTime.now());

            conversationLogMapper.insert(log);
            logger.info("对话记录保存成功 - 用户ID: {}, 查询类型: {}", userId, queryType);
            return log;
        } catch (Exception e) {
            logger.error("保存对话记录失败 - 错误: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<AiConversationLog> getHistoryByUserId(Long userId, Integer limit) {
        try {
            int actualLimit = limit != null && limit > 0 ? limit : 50;
            List<AiConversationLog> logs = conversationLogMapper.selectByUserId(userId, actualLimit);
            logger.info("查询对话历史成功 - 用户ID: {}, 记录数: {}", userId, logs.size());
            return logs;
        } catch (Exception e) {
            logger.error("查询对话历史失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public AiConversationLog getLogById(Long id) {
        try {
            return conversationLogMapper.selectById(id);
        } catch (Exception e) {
            logger.error("查询对话记录失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteLogById(Long id) {
        try {
            int result = conversationLogMapper.deleteById(id);
            boolean success = result > 0;
            logger.info("删除对话记录 - ID: {}, 成功: {}", id, success);
            return success;
        } catch (Exception e) {
            logger.error("删除对话记录失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean clearHistoryByUserId(Long userId) {
        try {
            int result = conversationLogMapper.deleteByUserId(userId);
            boolean success = result >= 0;
            logger.info("清空对话历史 - 用户ID: {}, 删除记录数: {}", userId, result);
            return success;
        } catch (Exception e) {
            logger.error("清空对话历史失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int countByUserId(Long userId) {
        try {
            return conversationLogMapper.countByUserId(userId);
        } catch (Exception e) {
            logger.error("统计对话记录数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }
}