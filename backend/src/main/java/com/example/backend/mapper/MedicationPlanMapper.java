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

    /**
     * 查询用户一周内所有用药计划（包括已删除的）
     */
    List<MedicationPlan> selectUserWeeklyPlans(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}