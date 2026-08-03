package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.ElderNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 老人端通知Mapper
 */
@Mapper
public interface ElderNotificationMapper extends BaseMapper<ElderNotification> {
}
