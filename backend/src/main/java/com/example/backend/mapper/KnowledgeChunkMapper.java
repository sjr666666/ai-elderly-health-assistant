package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 知识库切片 Mapper
 * 基于 MyBatis-Plus BaseMapper，无需额外 XML
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {
}
