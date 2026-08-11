package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.RagFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 回答质量反馈 Mapper
 * 基于 MyBatis-Plus BaseMapper，无需额外 XML
 */
@Mapper
public interface RagFeedbackMapper extends BaseMapper<RagFeedback> {
}
