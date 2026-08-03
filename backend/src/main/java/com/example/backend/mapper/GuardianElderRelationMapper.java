package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.GuardianElderRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 监护关系 Mapper 接口
 */
@Mapper
public interface GuardianElderRelationMapper extends BaseMapper<GuardianElderRelation> {
}
