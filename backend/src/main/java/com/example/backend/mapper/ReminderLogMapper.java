package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.ReminderLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ReminderLogMapper extends BaseMapper<ReminderLog> {
    /**
     * 查询用户未读的提醒记录（status='sent'）
     */
    List<ReminderLog> selectUnreadReminders(@Param("userId") Long userId);
}
