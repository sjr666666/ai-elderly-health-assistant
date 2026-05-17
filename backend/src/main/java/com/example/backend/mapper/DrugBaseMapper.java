package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.DrugBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品基础库 Mapper 接口
 */
@Mapper
public interface DrugBaseMapper extends BaseMapper<DrugBase> {
}