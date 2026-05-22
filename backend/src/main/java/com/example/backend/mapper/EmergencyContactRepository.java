package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.EmergencyContact;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmergencyContactRepository extends BaseMapper<EmergencyContact> {
}
