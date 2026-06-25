package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.MedicationWeeklyReport;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药周报Mapper接口
 */
public interface MedicationWeeklyReportMapper extends BaseMapper<MedicationWeeklyReport> {

    /**
     * 查询用户指定时间范围内的周报列表
     * 
     * @param userId 用户ID
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 周报列表
     */
    List<MedicationWeeklyReport> selectByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询用户最新的周报
     * 
     * @param userId 用户ID
     * @return 最新周报
     */
    MedicationWeeklyReport selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 统计用户的平均合规率
     * 
     * @param userId 用户ID
     * @return 平均合规率
     */
    Double selectAverageComplianceRate(@Param("userId") Long userId);
}
