package com.example.backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.UserMedicineBoxMapper;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.UserMedicineBox;
import com.example.backend.service.DailyLessonService;
import com.example.backend.service.ProgressiveReminderService;
import com.example.backend.config.ScheduledLock.DistributedTaskLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.ZoneId;

/**
 * 定时任务组件
 * 用于处理后台周期性任务
 */
@Component
public class ScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private UserMedicineBoxMapper userMedicineBoxMapper;

    @Autowired
    private MedicationPlanMapper medicationPlanMapper;

    @Autowired
    private DailyLessonService dailyLessonService;

    @Autowired
    private ProgressiveReminderService progressiveReminderService;

    @Autowired
    private DistributedTaskLock distributedTaskLock;

    /**
     * 每天凌晨1点执行：自动将已过期的药品状态更新为stopped
     * Cron表达式：0 0 1 * * ? 表示每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void autoExpireMedicines() {
        String lockToken = acquireLock("auto-expire-medicines");
        if (lockToken == null) return;
        logger.info("=== 开始执行药品过期检查定时任务 ===");
        
        try {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            logger.info("当前日期: {}", today);
            
            // 查询所有active状态的药品
            LambdaQueryWrapper<UserMedicineBox> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode());
            
            List<UserMedicineBox> activeMedicines = userMedicineBoxMapper.selectList(queryWrapper);
            logger.info("查询到{}个active状态的药品", activeMedicines.size());
            
            int expiredCount = 0;
            List<Map<String, Object>> newlyExpiredList = new ArrayList<>();
            
            // 遍历检查每个药品是否过期
            for (UserMedicineBox medicine : activeMedicines) {
                if (medicine.getExpiryDate() != null) {
                    // 如果有效期小于或等于今天，则标记为已过期
                    if (!medicine.getExpiryDate().isAfter(today)) {
                        // 更新status为stopped
                        LambdaUpdateWrapper<UserMedicineBox> updateWrapper = new LambdaUpdateWrapper<>();
                        updateWrapper.eq(UserMedicineBox::getId, medicine.getId())
                                   .set(UserMedicineBox::getStatus, UserMedicineBox.Status.STOPPED.getCode());
                        
                        int result = userMedicineBoxMapper.update(null, updateWrapper);
                        if (result > 0) {
                            expiredCount++;
                            logger.info("药品已过期，已更新为stopped状态 - boxId: {}, drugName: {}, expiryDate: {}",
                                    medicine.getId(), 
                                    medicine.getNote() != null ? medicine.getNote() : "未知药品",
                                    medicine.getExpiryDate());
                            
                            // 记录新过期的药品信息（用于前端提醒）
                            Map<String, Object> expiredInfo = new HashMap<>();
                            expiredInfo.put("boxItemId", medicine.getId());
                            expiredInfo.put("drugName", medicine.getNote() != null ? medicine.getNote() : "未知药品");
                            expiredInfo.put("expiryDate", medicine.getExpiryDate().toString());
                            expiredInfo.put("userId", medicine.getUserId());
                            newlyExpiredList.add(expiredInfo);
                        }
                    }
                }
            }
            
            // TODO: 可以将newlyExpiredList存储到Redis或数据库的通知表中
            // 暂时只记录日志，前端通过其他方式检测
            if (!newlyExpiredList.isEmpty()) {
                logger.info("=== 今日新过期药品列表 ===");
                for (Map<String, Object> expired : newlyExpiredList) {
                    logger.info("用户ID: {}, 药品: {}, 有效期: {}", 
                            expired.get("userId"), 
                            expired.get("drugName"), 
                            expired.get("expiryDate"));
                }
            }
            
            logger.info("=== 药品过期检查完成 === 共更新{}个过期药品", expiredCount);
            
        } catch (Exception e) {
            logger.error("药品过期检查定时任务执行失败", e);
        } finally {
            releaseLock("auto-expire-medicines", lockToken);
        }
    }

    /**
     * 每天凌晨0点执行：根据前一天的用药日历生成新的用药日历
     * Cron表达式：0 0 0 * * ? 表示每天凌晨0点执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void generateNextDayMedicationPlan() {
        String lockToken = acquireLock("generate-next-day-plan");
        if (lockToken == null) return;
        logger.info("=== 开始执行生成下一天用药计划定时任务 ===");
        
        try {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            LocalDate yesterday = today.minusDays(1);
            logger.info("当前日期: {}, 前一天: {}", today, yesterday);
            
            // 查询所有用户昨天的用药计划（不区分deleted状态，因为需要复制）
            LambdaQueryWrapper<MedicationPlan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MedicationPlan::getPlanDate, yesterday);
            
            List<MedicationPlan> yesterdayPlans = medicationPlanMapper.selectList(queryWrapper);
            logger.info("查询到{}个昨天的用药计划", yesterdayPlans.size());
            
            if (yesterdayPlans.isEmpty()) {
                logger.info("昨天没有用药计划，跳过生成");
                return;
            }
            
            int successCount = 0;
            int skippedCount = 0;
            int expiredCount = 0;
            int endedCount = 0;
            int failedCount = 0;
            
            // 按用户分组处理
            Map<Long, List<MedicationPlan>> plansByUser = new HashMap<>();
            for (MedicationPlan plan : yesterdayPlans) {
                plansByUser.computeIfAbsent(plan.getUserId(), k -> new ArrayList<>()).add(plan);
            }
            
            // 为每个用户生成今天的用药计划
            for (Map.Entry<Long, List<MedicationPlan>> entry : plansByUser.entrySet()) {
                Long userId = entry.getKey();
                List<MedicationPlan> userPlans = entry.getValue();
                
                logger.info("为用户 {} 生成用药计划，昨天有 {} 个计划", userId, userPlans.size());
                
                for (MedicationPlan yesterdayPlan : userPlans) {
                    try {
                        // 检查对应的药箱条目是否有效
                        if (yesterdayPlan.getBoxItemId() != null) {
                            UserMedicineBox boxItem = userMedicineBoxMapper.selectById(yesterdayPlan.getBoxItemId());
                            
                            if (boxItem == null) {
                                logger.warn("药箱条目不存在，跳过 - boxItemId: {}", yesterdayPlan.getBoxItemId());
                                skippedCount++;
                                continue;
                            }
                            
                            // 检查1：药物是否已停用
                            if (!UserMedicineBox.Status.ACTIVE.getCode().equals(boxItem.getStatus())) {
                                logger.info("药物已停用，不加入今日计划 - boxItemId: {}, status: {}", 
                                        boxItem.getId(), boxItem.getStatus());
                                skippedCount++;
                                continue;
                            }
                            
                            // 检查2：结束服药日期是否已过
                            if (boxItem.getEndDate() != null && boxItem.getEndDate().isBefore(today)) {
                                logger.info("结束服药日期已过，不加入今日计划 - boxItemId: {}, endDate: {}", 
                                        boxItem.getId(), boxItem.getEndDate());
                                endedCount++;
                                continue;
                            }
                            
                            // 检查3：药物是否已过期
                            if (boxItem.getExpiryDate() != null && !boxItem.getExpiryDate().isAfter(today)) {
                                logger.info("药物已过期，不加入今日计划 - boxItemId: {}, expiryDate: {}", 
                                        boxItem.getId(), boxItem.getExpiryDate());
                                expiredCount++;
                                continue;
                            }
                            
                            // 检查4：开始服药日期是否已到
                            if (boxItem.getStartDate() != null && boxItem.getStartDate().isAfter(today)) {
                                logger.info("开始服药日期未到，不加入今日计划 - boxItemId: {}, startDate: {}", 
                                        boxItem.getId(), boxItem.getStartDate());
                                skippedCount++;
                                continue;
                            }
                        }
                        
                        // 创建新的用药计划
                        MedicationPlan newPlan = new MedicationPlan();
                        newPlan.setUserId(yesterdayPlan.getUserId());
                        newPlan.setDrugId(yesterdayPlan.getDrugId());
                        newPlan.setBoxItemId(yesterdayPlan.getBoxItemId());
                        newPlan.setPlanDate(today);
                        newPlan.setTimeSlot(yesterdayPlan.getTimeSlot());
                        newPlan.setDosageAtTime(yesterdayPlan.getDosageAtTime());
                        newPlan.setStatus(MedicationPlan.Status.PENDING.getCode()); // 新计划状态为待服用
                        newPlan.setRemindBefore(yesterdayPlan.getRemindBefore());
                        
                        int result = medicationPlanMapper.insert(newPlan);
                        if (result > 0) {
                            successCount++;
                            logger.debug("成功创建用药计划 - planId: {}, drugId: {}, timeSlot: {}", 
                                    newPlan.getId(), newPlan.getDrugId(), newPlan.getTimeSlot());
                        }
                        
                    } catch (Exception e) {
                        // 单条失败不影响其他用户，但必须计数并在日志中完整记录，便于事后补偿
                        failedCount++;
                        logger.error("创建用药计划失败 - yesterdayPlanId: {}, userId: {}",
                                yesterdayPlan.getId(), yesterdayPlan.getUserId(), e);
                    }
                }
            }
            
            logger.info("=== 生成下一天用药计划完成 === 成功: {}, 跳过: {}, 已结束: {}, 已过期: {}, 失败: {}", 
                    successCount, skippedCount, endedCount, expiredCount, failedCount);
            if (failedCount > 0) {
                logger.warn("有 {} 条用药计划生成失败，请检查上方错误日志并手动补偿", failedCount);
            }
            
        } catch (Exception e) {
            logger.error("生成下一天用药计划定时任务执行失败", e);
        } finally {
            releaseLock("generate-next-day-plan", lockToken);
        }
    }

    /**
     * 每天早晨6:00执行：为所有有慢病史的用户预生成今日科普
     * 让用户在打开App时就能看到已生成的内容，无需等待AI响应
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void generateDailyLessons() {
        String lockToken = acquireLock("generate-daily-lessons");
        if (lockToken == null) return;
        logger.info("=== 开始执行今日一课预生成定时任务 ===");
        try {
            dailyLessonService.generateDailyLessons();
            logger.info("=== 今日一课预生成定时任务完成 ===");
        } catch (Exception e) {
            logger.error("今日一课预生成定时任务执行失败", e);
        } finally {
            releaseLock("generate-daily-lessons", lockToken);
        }
    }

    /**
     * 每分钟执行：渐进式提醒扫描
     * 阶段推进：none → pre_remind(提前15min) → due_now(到时) → overdue(超时) → notify_family(超时10min通知家属)
     */
    @Scheduled(cron = "0 * * * * ?")
    public void progressiveReminderCheck() {
        String lockToken = acquireLock("progressive-reminders");
        if (lockToken == null) return;
        try {
            progressiveReminderService.processProgressiveReminders();
        } catch (Exception e) {
            logger.error("渐进式提醒定时任务执行失败", e);
        } finally {
            releaseLock("progressive-reminders", lockToken);
        }
    }

    private String acquireLock(String name) {
        try {
            return distributedTaskLock.tryAcquire(name, Duration.ofMinutes(5));
        } catch (RuntimeException exception) {
            logger.warn("Unable to acquire scheduled task lock: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    private void releaseLock(String name, String token) {
        try {
            distributedTaskLock.release(name, token);
        } catch (RuntimeException exception) {
            logger.warn("Unable to release scheduled task lock: {}", exception.getClass().getSimpleName());
        }
    }
}
