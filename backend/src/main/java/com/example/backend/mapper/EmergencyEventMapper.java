package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.EmergencyEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmergencyEventMapper extends BaseMapper<EmergencyEvent> {
}
