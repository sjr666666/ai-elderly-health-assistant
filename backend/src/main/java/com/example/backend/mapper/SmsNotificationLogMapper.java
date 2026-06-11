package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.SmsNotificationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsNotificationLogMapper extends BaseMapper<SmsNotificationLog> {
}
