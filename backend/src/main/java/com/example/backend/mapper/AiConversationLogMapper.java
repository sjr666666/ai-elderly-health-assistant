package com.example.backend.mapper;

import com.example.backend.model.entity.AiConversationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话记录Mapper接口
 */
@Mapper
public interface AiConversationLogMapper {

    /**
     * 插入对话记录
     *
     * @param log 对话记录实体
     * @return 影响行数
     */
    int insert(AiConversationLog log);

    /**
     * 根据用户ID查询对话记录列表
     *
     * @param userId 用户ID
     * @param limit  查询数量限制
     * @return 对话记录列表
     */
    List<AiConversationLog> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 根据ID查询对话记录
     *
     * @param id 记录ID
     * @return 对话记录
     */
    AiConversationLog selectById(@Param("id") Long id);

    /**
     * 根据ID删除对话记录（逻辑删除）
     *
     * @param id 记录ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID删除所有对话记录（逻辑删除）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 统计用户对话记录数量
     *
     * @param userId 用户ID
     * @return 记录数量
     */
    int countByUserId(@Param("userId") Long userId);
}