package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.MedicationLog;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

public interface MedicationLogMapper extends BaseMapper<MedicationLog> {
    /**
     * 查询用户指定日期的所有服药记录
     * 通过关联用药计划表查询日期
     */
    List<MedicationLog> selectUserDailyLogs(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);
}
