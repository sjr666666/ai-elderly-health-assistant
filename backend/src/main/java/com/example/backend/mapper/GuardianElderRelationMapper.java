package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.GuardianElderRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监护关系 Mapper 接口
 */
@Mapper
public interface GuardianElderRelationMapper extends BaseMapper<GuardianElderRelation> {

    /**
     * 根据监护人ID查询关联关系
     *
     * @param guardianId 监护人ID
     * @return 关联关系列表
     */
    List<GuardianElderRelation> findByGuardianId(@Param("guardianId") Long guardianId);

    /**
     * 根据老人ID查询关联关系（用于老人端查询自己的家属列表）
     *
     * @param elderId 老人ID
     * @return 关联关系列表
     */
    List<GuardianElderRelation> findByElderId(@Param("elderId") Long elderId);
}
