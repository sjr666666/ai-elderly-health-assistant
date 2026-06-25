package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.MedicationLogMapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.MedicationWeeklyReportMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.WeeklyReportRequest;
import com.example.backend.model.dto.WeeklyReportResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.MedicationLog;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.MedicationWeeklyReport;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.WeeklyReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用药周报服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final MedicationPlanMapper medicationPlanMapper;
    private final MedicationLogMapper medicationLogMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final MedicationWeeklyReportMapper weeklyReportMapper;
    private final DeepSeekService deepSeekService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeeklyReportResponse generateWeeklyReport(WeeklyReportRequest request) {
        log.info("开始生成用药周报 - userId: {}, startDate: {}, endDate: {}", 
                request.getUserId(), request.getStartDate(), request.getEndDate());

        // 1. 将前端传入的 user_id (如 10001) 转换为数据库中的实际 ID (如 1)
        Long actualUserId = convertToActualUserId(request.getUserId());
        if (actualUserId == null) {
            log.warn("用户不存在 - userId: {}", request.getUserId());
            return buildEmptyReport(request.getUserId(), 
                    request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusDays(6),
                    request.getEndDate() != null ? request.getEndDate() : LocalDate.now());
        }

        // 2. 设置默认日期范围（最近7天）
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : endDate.minusDays(6);

        // 3. 查询该用户一周内的所有用药计划
        List<MedicationPlan> weeklyPlans = medicationPlanMapper.selectUserWeeklyPlans(
                actualUserId, startDate, endDate);

        if (weeklyPlans.isEmpty()) {
            log.warn("该用户在指定日期范围内无用药计划 - userId: {} (actual: {})", request.getUserId(), actualUserId);
            return buildEmptyReport(request.getUserId(), startDate, endDate);
        }

        // 4. 获取药品信息映射
        Set<Long> drugIds = weeklyPlans.stream()
                .map(MedicationPlan::getDrugId)
                .collect(Collectors.toSet());
        Map<Long, DrugBase> drugMap = drugBaseMapper.selectBatchIds(drugIds).stream()
                .collect(Collectors.toMap(DrugBase::getId, drug -> drug));

        // 5. 获取用药记录（通过关联plan表查询指定日期范围内的记录）
        List<MedicationLog> logs = medicationLogMapper.selectList(new LambdaQueryWrapper<MedicationLog>()
                .eq(MedicationLog::getUserId, actualUserId)
                .inSql(MedicationLog::getPlanId,
                    "SELECT id FROM medication_plan WHERE user_id = " + actualUserId +
                    " AND plan_date >= '" + startDate + "' AND plan_date <= '" + endDate + "'")
        );

        Map<Long, String> planStatusMap = logs.stream()
                .collect(Collectors.toMap(MedicationLog::getPlanId, MedicationLog::getStatus, (v1, v2) -> v1));

        // 6. 统计总体数据
        WeeklyReportResponse.ReportStatistics statistics = calculateStatistics(weeklyPlans, planStatusMap, drugMap);

        // 7. 按日期分组统计
        List<WeeklyReportResponse.DailySummary> dailySummaries = calculateDailySummaries(
                weeklyPlans, planStatusMap, drugMap, startDate, endDate);

        // 8. 计算漏服药品列表
        List<String> missedDrugs = calculateMissedDrugs(weeklyPlans, planStatusMap, drugMap);

        // 9. 计算最佳和最差时段
        Map<String, double[]> timeSlotStats = calculateTimeSlotStats(weeklyPlans, planStatusMap);
        String bestTimeSlot = findBestTimeSlot(timeSlotStats);
        String needsImprovementTimeSlot = findWorstTimeSlot(timeSlotStats);

        // 10. 生成AI总结
        String aiSummary = generateAISummary(statistics, dailySummaries, missedDrugs, bestTimeSlot, needsImprovementTimeSlot);

        // 11. 生成完整报告文本（用于截图展示）
        String fullReportText = generateFullReportText(statistics, dailySummaries, missedDrugs, 
                bestTimeSlot, needsImprovementTimeSlot, aiSummary);

        // 12. 构建响应
        WeeklyReportResponse report = WeeklyReportResponse.builder()
                .reportId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .userId(request.getUserId())
                .startDate(startDate)
                .endDate(endDate)
                .statistics(statistics)
                .dailySummaries(dailySummaries)
                .aiSummary(aiSummary)
                .missedDrugs(missedDrugs)
                .bestTimeSlot(bestTimeSlot)
                .needsImprovementTimeSlot(needsImprovementTimeSlot)
                .fullReportText(fullReportText)
                .build();

        // 13. 保存周报到数据库
        saveReportToDatabase(report, statistics, dailySummaries, missedDrugs);

        return report;
    }

    /**
     * 计算总体统计信息
     */
    private WeeklyReportResponse.ReportStatistics calculateStatistics(
            List<MedicationPlan> plans, Map<Long, String> planStatusMap, Map<Long, DrugBase> drugMap) {
        
        int totalPlans = plans.size();
        int takenCount = 0;
        int missedCount = 0;
        int skippedCount = 0;
        int pendingCount = 0;
        LocalDate today = LocalDate.now();

        for (MedicationPlan plan : plans) {
            // 优先使用用药记录的状态，如果没有记录则根据计划日期判断
            String status = planStatusMap.containsKey(plan.getId()) 
                    ? planStatusMap.get(plan.getId()) 
                    : determinePlanStatus(plan, today);
            
            switch (status.toLowerCase()) {
                case "taken":
                case "completed":
                    takenCount++;
                    break;
                case "missed":
                    missedCount++;
                    break;
                case "skipped":
                case "cancelled":
                    skippedCount++;
                    break;
                default:
                    pendingCount++;
                    break;
            }
        }

        int completedCount = takenCount + skippedCount;
        double complianceRate = totalPlans > 0 ? (double) completedCount / totalPlans * 100 : 0;
        long drugVarietyCount = plans.stream().map(MedicationPlan::getDrugId).distinct().count();

        return WeeklyReportResponse.ReportStatistics.builder()
                .totalPlans(totalPlans)
                .takenCount(takenCount)
                .missedCount(missedCount)
                .skippedCount(skippedCount)
                .pendingCount(pendingCount)
                .complianceRate(Math.round(complianceRate * 100.0) / 100.0)
                .drugVarietyCount((int) drugVarietyCount)
                .build();
    }

    /**
     * 根据计划日期判断状态
     * - 日期已过且无记录 → missed
     * - 日期未过且无记录 → pending
     */
    private String determinePlanStatus(MedicationPlan plan, LocalDate today) {
        if (plan.getPlanDate() == null) {
            return "pending";
        }
        
        // 如果计划日期在今天之前，且没有用药记录，视为漏服
        if (plan.getPlanDate().isBefore(today)) {
            return "missed";
        }
        
        // 如果是今天或未来的日期，且没有记录，视为待服用
        return "pending";
    }

    /**
     * 计算每日汇总
     */
    private List<WeeklyReportResponse.DailySummary> calculateDailySummaries(
            List<MedicationPlan> plans, Map<Long, String> planStatusMap, 
            Map<Long, DrugBase> drugMap, LocalDate startDate, LocalDate endDate) {
        
        List<WeeklyReportResponse.DailySummary> summaries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date; // 使用final变量
            List<MedicationPlan> dayPlans = plans.stream()
                    .filter(p -> p.getPlanDate().equals(currentDate))
                    .collect(Collectors.toList());

            if (dayPlans.isEmpty()) {
                continue;
            }

            int totalPlans = dayPlans.size();
            int takenCount = 0;
            int missedCount = 0;
            Set<String> drugs = new HashSet<>();

            for (MedicationPlan plan : dayPlans) {
                // 优先使用用药记录的状态，如果没有记录则根据计划日期判断
                String status = planStatusMap.containsKey(plan.getId()) 
                        ? planStatusMap.get(plan.getId()) 
                        : determinePlanStatus(plan, today);
                
                if ("taken".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                    takenCount++;
                } else if ("missed".equalsIgnoreCase(status)) {
                    missedCount++;
                }

                DrugBase drug = drugMap.get(plan.getDrugId());
                if (drug != null) {
                    drugs.add(drug.getCommonName());
                }
            }

            double complianceRate = totalPlans > 0 ? (double) takenCount / totalPlans * 100 : 0;
            String dayOfWeek = getDayOfWeek(date.getDayOfWeek().getValue());

            summaries.add(WeeklyReportResponse.DailySummary.builder()
                    .date(date)
                    .dayOfWeek(dayOfWeek)
                    .totalPlans(totalPlans)
                    .takenCount(takenCount)
                    .missedCount(missedCount)
                    .complianceRate(Math.round(complianceRate * 100.0) / 100.0)
                    .drugs(new ArrayList<>(drugs))
                    .build());
        }

        return summaries;
    }

    /**
     * 计算漏服药品列表
     */
    private List<String> calculateMissedDrugs(
            List<MedicationPlan> plans, Map<Long, String> planStatusMap, Map<Long, DrugBase> drugMap) {
        LocalDate today = LocalDate.now();
        
        return plans.stream()
                .filter(plan -> {
                    // 优先使用用药记录的状态，如果没有记录则根据计划日期判断
                    String status = planStatusMap.containsKey(plan.getId()) 
                            ? planStatusMap.get(plan.getId()) 
                            : determinePlanStatus(plan, today);
                    return "missed".equalsIgnoreCase(status);
                })
                .map(plan -> drugMap.get(plan.getDrugId()))
                .filter(Objects::nonNull)
                .map(DrugBase::getCommonName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 计算各时段统计
     */
    private Map<String, double[]> calculateTimeSlotStats(
            List<MedicationPlan> plans, Map<Long, String> planStatusMap) {
        
        Map<String, double[]> stats = new HashMap<>();
        
        for (MedicationPlan plan : plans) {
            String timeSlot = plan.getTimeSlot();
            String status = planStatusMap.getOrDefault(plan.getId(), plan.getStatus());
            
            stats.putIfAbsent(timeSlot, new double[]{0, 0}); // [total, taken]
            double[] slotStats = stats.get(timeSlot);
            slotStats[0]++;
            
            if ("taken".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                slotStats[1]++;
            }
        }
        
        return stats;
    }

    /**
     * 找出按时服药率最高的时段
     */
    private String findBestTimeSlot(Map<String, double[]> timeSlotStats) {
        return timeSlotStats.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > 0)
                .max(Comparator.comparingDouble(entry -> entry.getValue()[1] / entry.getValue()[0]))
                .map(Map.Entry::getKey)
                .map(this::getTimeSlotLabel)
                .orElse("无数据");
    }

    /**
     * 找出需要改进的时段
     */
    private String findWorstTimeSlot(Map<String, double[]> timeSlotStats) {
        return timeSlotStats.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > 0)
                .min(Comparator.comparingDouble(entry -> entry.getValue()[1] / entry.getValue()[0]))
                .map(Map.Entry::getKey)
                .map(this::getTimeSlotLabel)
                .orElse("无数据");
    }

    /**
     * 生成AI总结
     */
    private String generateAISummary(WeeklyReportResponse.ReportStatistics statistics,
                                     List<WeeklyReportResponse.DailySummary> dailySummaries,
                                     List<String> missedDrugs,
                                     String bestTimeSlot,
                                     String needsImprovementTimeSlot) {
        
        try {
            // 构建AI提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下用药数据生成一份简洁专业的用药周报总结（200字以内）：\n\n");
            prompt.append(String.format("总体情况：本周共%d次用药计划，已完成%d次，漏服%d次，跳过%d次，按时服药率%.1f%%\n",
                    statistics.getTotalPlans(), statistics.getTakenCount(), 
                    statistics.getMissedCount(), statistics.getSkippedCount(),
                    statistics.getComplianceRate()));
            prompt.append(String.format("涉及药品种类：%d种\n", statistics.getDrugVarietyCount()));
            
            if (!missedDrugs.isEmpty()) {
                prompt.append(String.format("漏服药品：%s\n", String.join("、", missedDrugs)));
            }
            
            prompt.append(String.format("表现最好时段：%s\n", bestTimeSlot));
            prompt.append(String.format("需改进时段：%s\n\n", needsImprovementTimeSlot));
            
            prompt.append("请给出：\n");
            prompt.append("1. 对本周用药依从性的评价\n");
            prompt.append("2. 针对漏服情况的建议\n");
            prompt.append("3. 鼓励性话语\n");

            // 调用DeepSeek API生成总结
            // 注意：这里简化处理，实际应该调用deepSeekService的方法
            String aiResponse = callDeepSeekForSummary(prompt.toString());
            
            return aiResponse != null && !aiResponse.isEmpty() ? aiResponse : getDefaultSummary(statistics);
            
        } catch (Exception e) {
            log.error("AI总结生成失败，使用默认总结", e);
            return getDefaultSummary(statistics);
        }
    }

    /**
     * 调用DeepSeek生成总结（简化版）
     */
    private String callDeepSeekForSummary(String prompt) {
        try {
            // 这里可以调用deepSeekService的通用方法
            // 由于DeepSeekService主要针对冲突检测，这里先返回null使用默认总结
            // 后续可以扩展DeepSeekService添加通用对话功能
            log.info("AI总结提示词：{}", prompt);
            return null; // 暂时返回null，使用默认总结
        } catch (Exception e) {
            log.error("调用DeepSeek失败", e);
            return null;
        }
    }

    /**
     * 生成默认总结
     */
    private String getDefaultSummary(WeeklyReportResponse.ReportStatistics statistics) {
        StringBuilder summary = new StringBuilder();
        
        double rate = statistics.getComplianceRate();
        if (rate >= 90) {
            summary.append("【优秀】本周用药依从性非常好！继续保持规律服药的习惯。\n");
        } else if (rate >= 70) {
            summary.append("【良好】本周用药情况较好，但仍有提升空间。建议设置提醒，避免漏服。\n");
        } else if (rate >= 50) {
            summary.append("【一般】本周漏服次数较多，建议调整用药时间或使用药盒辅助。\n");
        } else {
            summary.append("【需改进】本周漏服严重，建议咨询医生调整用药方案或寻求家属协助监督。\n");
        }
        
        if (statistics.getMissedCount() > 0) {
            summary.append(String.format("\n本周共漏服%d次，建议设置手机闹钟或使用智能药盒提醒。\n", 
                    statistics.getMissedCount()));
        }
        
        summary.append("\n坚持规律服药是控制病情的关键，祝您健康！");
        
        return summary.toString();
    }

    /**
     * 生成完整报告文本（用于截图展示）
     */
    private String generateFullReportText(WeeklyReportResponse.ReportStatistics statistics,
                                          List<WeeklyReportResponse.DailySummary> dailySummaries,
                                          List<String> missedDrugs,
                                          String bestTimeSlot,
                                          String needsImprovementTimeSlot,
                                          String aiSummary) {
        
        StringBuilder report = new StringBuilder();
        report.append("=" .repeat(40)).append("\n");
        report.append("       AI用药周报\n");
        report.append("=" .repeat(40)).append("\n\n");
        
        // 总体统计
        report.append("📊 总体统计\n");
        report.append("-".repeat(40)).append("\n");
        report.append(String.format("总计划数：%d次\n", statistics.getTotalPlans()));
        report.append(String.format("已服用：%d次\n", statistics.getTakenCount()));
        report.append(String.format("漏服：%d次\n", statistics.getMissedCount()));
        report.append(String.format("跳过：%d次\n", statistics.getSkippedCount()));
        report.append(String.format("按时服药率：%.1f%%\n", statistics.getComplianceRate()));
        report.append(String.format("药品种类：%d种\n\n", statistics.getDrugVarietyCount()));
        
        // 时段分析
        report.append("⏰ 时段分析\n");
        report.append("-".repeat(40)).append("\n");
        report.append(String.format("表现最好：%s\n", bestTimeSlot));
        report.append(String.format("需改进：%s\n\n", needsImprovementTimeSlot));
        
        // 漏服药品
        if (!missedDrugs.isEmpty()) {
            report.append("⚠️ 漏服药品\n");
            report.append("-".repeat(40)).append("\n");
            for (String drug : missedDrugs) {
                report.append("• ").append(drug).append("\n");
            }
            report.append("\n");
        }
        
        // 每日详情
        report.append("📅 每日详情\n");
        report.append("-".repeat(40)).append("\n");
        for (WeeklyReportResponse.DailySummary daily : dailySummaries) {
            report.append(String.format("%s (%s): %d/%d次 (%.0f%%)\n",
                    daily.getDate(), daily.getDayOfWeek(), 
                    daily.getTakenCount(), daily.getTotalPlans(),
                    daily.getComplianceRate()));
        }
        report.append("\n");
        
        // AI建议
        report.append("💡 AI建议\n");
        report.append("-".repeat(40)).append("\n");
        report.append(aiSummary);
        report.append("\n\n");
        
        report.append("=" .repeat(40)).append("\n");
        report.append("数据来源：AI药管家\n");
        report.append("生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("=" .repeat(40));
        
        return report.toString();
    }

    /**
     * 构建空报告
     */
    private WeeklyReportResponse buildEmptyReport(Long userId, LocalDate startDate, LocalDate endDate) {
        WeeklyReportResponse.ReportStatistics emptyStats = WeeklyReportResponse.ReportStatistics.builder()
                .totalPlans(0)
                .takenCount(0)
                .missedCount(0)
                .skippedCount(0)
                .pendingCount(0)
                .complianceRate(0.0)
                .drugVarietyCount(0)
                .build();

        return WeeklyReportResponse.builder()
                .reportId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .statistics(emptyStats)
                .dailySummaries(new ArrayList<>())
                .aiSummary("本周暂无用药记录，请先在药箱中添加药品并设置用药计划。")
                .missedDrugs(new ArrayList<>())
                .bestTimeSlot("无数据")
                .needsImprovementTimeSlot("无数据")
                .fullReportText("本周暂无用药记录")
                .build();
    }

    /**
     * 将前端传入的 user_id (如 10001) 转换为数据库中的实际 ID (如 1)
     */
    private Long convertToActualUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(wrapper);
        
        return user != null ? user.getId() : null;
    }

    /**
     * 获取星期几的中文表示
     */
    private String getDayOfWeek(int dayOfWeek) {
        String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[dayOfWeek];
    }

    /**
     * 获取时段的中文标签
     */
    private String getTimeSlotLabel(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> "早上";
            case "noon" -> "中午";
            case "afternoon" -> "下午";
            case "evening" -> "晚上";
            case "before_bed" -> "睡前";
            case "night" -> "睡前";
            default -> timeSlot;
        };
    }

    /**
     * 保存周报到数据库
     */
    @Transactional(rollbackFor = Exception.class)
    private void saveReportToDatabase(
            WeeklyReportResponse report,
            WeeklyReportResponse.ReportStatistics statistics,
            List<WeeklyReportResponse.DailySummary> dailySummaries,
            List<String> missedDrugs) {
        
        try {
            MedicationWeeklyReport entity = new MedicationWeeklyReport();
            entity.setReportId(report.getReportId());
            entity.setUserId(report.getUserId());
            entity.setStartDate(report.getStartDate());
            entity.setEndDate(report.getEndDate());
            
            // JSON字段序列化 - 需要将LocalDate转换为字符串
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            
            entity.setStatisticsJson(jsonMapper.writeValueAsString(statistics));
            entity.setDailySummariesJson(jsonMapper.writeValueAsString(dailySummaries));
            entity.setMissedDrugsJson(jsonMapper.writeValueAsString(missedDrugs));
            
            // AI生成内容
            entity.setAiSummary(report.getAiSummary());
            entity.setFullReportText(report.getFullReportText());
            
            // 关键指标（冗余字段）
            entity.setTotalPlans(statistics.getTotalPlans());
            entity.setTakenCount(statistics.getTakenCount());
            entity.setMissedCount(statistics.getMissedCount());
            entity.setSkippedCount(statistics.getSkippedCount());
            entity.setComplianceRate(java.math.BigDecimal.valueOf(statistics.getComplianceRate()));
            entity.setDrugVarietyCount(statistics.getDrugVarietyCount());
            
            // 时段分析
            entity.setBestTimeSlot(report.getBestTimeSlot());
            entity.setNeedsImprovementTimeSlot(report.getNeedsImprovementTimeSlot());
            
            // 时间戳
            entity.setGeneratedAt(report.getGeneratedAt());
            
            // 插入数据库
            weeklyReportMapper.insert(entity);
            
            log.info("用药周报已保存到数据库 - reportId: {}, userId: {}", 
                    report.getReportId(), report.getUserId());
            
        } catch (Exception e) {
            log.error("保存用药周报到数据库失败 - reportId: {}", report.getReportId(), e);
            // 不抛出异常，避免影响主流程
        }
    }
}
