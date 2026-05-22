package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.DrugRecognitionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品识别日志 Mapper 接口
 */
@Mapper
public interface DrugRecognitionLogMapper extends BaseMapper<DrugRecognitionLog> {
}