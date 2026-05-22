package com.example.backend.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.MedicationPlan;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

public interface MedicationPlanMapper extends BaseMapper<MedicationPlan> {
    /**
     * 查询用户指定日期的所有未删除用药计划
     */
    List<MedicationPlan> selectUserDailyPlans(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);
}