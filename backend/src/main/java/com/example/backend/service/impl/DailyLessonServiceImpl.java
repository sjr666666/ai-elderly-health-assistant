package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.mapper.DailyLessonMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.DailyLessonDTO;
import com.example.backend.model.entity.DailyLesson;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.DailyLessonService;
import com.example.backend.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 今日一课 - 慢病科普服务实现
 *
 * @author backend
 * @since 1.1.0
 */
@Service
public class DailyLessonServiceImpl implements DailyLessonService {

    private static final Logger logger = LoggerFactory.getLogger(DailyLessonServiceImpl.class);

    private final DailyLessonMapper dailyLessonMapper;
    private final UserMapper userMapper;
    private final DeepSeekService deepSeekService;

    public DailyLessonServiceImpl(DailyLessonMapper dailyLessonMapper,
                                  UserMapper userMapper,
                                  DeepSeekService deepSeekService) {
        this.dailyLessonMapper = dailyLessonMapper;
        this.userMapper = userMapper;
        this.deepSeekService = deepSeekService;
    }

    /**
     * 解析用户的慢病列表
     */
    private List<String> parseDiseases(String chronicDiseases) {
        if (chronicDiseases == null || chronicDiseases.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> diseases = new ArrayList<>();
        for (String part : chronicDiseases.split("[、,;，；/]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                diseases.add(trimmed);
            }
        }
        return diseases;
    }

    /**
     * 根据当日dayOfYear轮换选择一个慢病，避开当天已生成过的主题
     *
     * @param diseases        用户慢病列表
     * @param excludeDiseases 需排除的疾病（如当天已生成过的主题），可为空
     * @return 选中的慢病名称；无可用慢病时返回null
     */
    private String pickDiseaseForToday(List<String> diseases, List<String> excludeDiseases) {
        if (diseases.isEmpty()) {
            return null;
        }
        // 过滤掉需排除的疾病，避免同一天重复主题
        List<String> candidates = diseases.stream()
                .filter(d -> excludeDiseases == null || !excludeDiseases.contains(d))
                .collect(Collectors.toList());
        // 若全部已被排除（所有主题当天都用过），回退到全部慢病列表，保证总有内容可生成
        if (candidates.isEmpty()) {
            candidates = diseases;
        }
        int index = LocalDate.now().getDayOfYear() % candidates.size();
        return candidates.get(index);
    }

    /**
     * 将Entity转为DTO
     */
    private DailyLessonDTO toDTO(DailyLesson entity) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return DailyLessonDTO.builder()
                .id(entity.getId())
                .lessonDate(entity.getLessonDate() != null ? entity.getLessonDate().format(fmt) : null)
                .chronicDisease(entity.getChronicDisease())
                .title(entity.getTitle())
                .content(entity.getContent())
                .generated(entity.getIsGenerated() != null && entity.getIsGenerated() == 1)
                .errorMsg(entity.getErrorMsg())
                .build();
    }

    @Override
    public DailyLessonDTO getTodayLesson(Long userId) {
        // 查找用户
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            logger.warn("用户不存在 - userId: {}", userId);
            return DailyLessonDTO.builder()
                    .lessonDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .generated(false)
                    .errorMsg("用户不存在")
                    .build();
        }

        LocalDate today = LocalDate.now();
        logger.info("获取今日科普 - userId: {}, 日期: {}", userId, today);

        // 1. 检查用户是否有慢病史
        List<String> diseases = parseDiseases(user.getChronicDiseases());
        if (diseases.isEmpty()) {
            logger.info("用户 {} 无慢病史，返回提示信息", userId);
            return DailyLessonDTO.builder()
                    .lessonDate(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .generated(false)
                    .errorMsg("请先完善健康档案中的慢性病史信息")
                    .build();
        }

        // 2. 查询今日是否已有记录
        LambdaQueryWrapper<DailyLesson> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyLesson::getUserId, userId)
                    .eq(DailyLesson::getLessonDate, today);
        DailyLesson existing = dailyLessonMapper.selectOne(queryWrapper);

        // 3. 缓存命中
        if (existing != null && existing.getIsGenerated() != null && existing.getIsGenerated() == 1) {
            logger.info("缓存命中 - lessonId: {}", existing.getId());
            return toDTO(existing);
        }

        // 4. 上次生成失败，重试
        if (existing != null && existing.getIsGenerated() == 0) {
            logger.info("上次生成失败，重试 - lessonId: {}", existing.getId());
            return doGenerateAndSave(existing, user, Collections.emptyList());
        }

        // 5. 无记录，新建并生成
        logger.info("首次请求，插入占位行并生成");
        DailyLesson placeholder = new DailyLesson();
        placeholder.setUserId(userId);
        placeholder.setLessonDate(today);
        placeholder.setIsGenerated(0);
        dailyLessonMapper.insert(placeholder);

        return doGenerateAndSave(placeholder, user, Collections.emptyList());
    }

    /**
     * 执行AI生成并更新数据库
     *
     * @param excludeDiseases 需排除的疾病主题（如当天已生成过的），可为空
     */
    private DailyLessonDTO doGenerateAndSave(DailyLesson record, SysUser user, List<String> excludeDiseases) {
        List<String> diseases = parseDiseases(user.getChronicDiseases());
        String selectedDisease = pickDiseaseForToday(diseases, excludeDiseases);

        if (selectedDisease == null) {
            return DailyLessonDTO.builder()
                    .lessonDate(record.getLessonDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .generated(false)
                    .errorMsg("未找到慢病信息")
                    .build();
        }

        try {
            Map<String, String> result = deepSeekService.generateDiseaseScienceLesson(
                    selectedDisease, user.getAge(), user.getGender());

            if (result != null && result.containsKey("title") && result.containsKey("content")) {
                record.setChronicDisease(selectedDisease);
                record.setTitle(result.get("title"));
                record.setContent(result.get("content"));
                record.setIsGenerated(1);
                record.setErrorMsg(null);
                dailyLessonMapper.updateById(record);

                logger.info("今日科普生成成功 - userId: {}, 疾病: {}, 标题: {}",
                        user.getId(), selectedDisease, result.get("title"));
                return toDTO(record);
            } else {
                String errMsg = "AI返回结果为空";
                record.setErrorMsg(errMsg);
                record.setIsGenerated(0);
                dailyLessonMapper.updateById(record);
                logger.warn("今日科普生成失败（AI返回空） - userId: {}", user.getId());
                return toDTO(record);
            }
        } catch (Exception e) {
            String errMsg = "AI生成失败: " + e.getMessage();
            record.setErrorMsg(errMsg.length() > 500 ? errMsg.substring(0, 500) : errMsg);
            record.setIsGenerated(0);
            dailyLessonMapper.updateById(record);
            logger.error("今日科普AI生成异常 - userId: {}", user.getId(), e);
            return toDTO(record);
        }
    }

    @Override
    @Transactional
    public DailyLessonDTO regenerateTodayLesson(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return DailyLessonDTO.builder()
                    .lessonDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .generated(false)
                    .errorMsg("用户不存在")
                    .build();
        }

        LocalDate today = LocalDate.now();
        logger.info("强制重新生成今日科普 - userId: {}", userId);

        // 物理删除前，先查询当天已成功生成过的疾病主题，作为本次轮换的排除集合，
        // 避免同一天重复生成相同主题
        LambdaQueryWrapper<DailyLesson> todayGeneratedQuery = new LambdaQueryWrapper<>();
        todayGeneratedQuery.eq(DailyLesson::getUserId, userId)
                           .eq(DailyLesson::getLessonDate, today)
                           .eq(DailyLesson::getIsGenerated, 1)
                           .isNotNull(DailyLesson::getChronicDisease)
                           .ne(DailyLesson::getChronicDisease, "");
        List<String> excludeDiseases = dailyLessonMapper.selectList(todayGeneratedQuery).stream()
                .map(DailyLesson::getChronicDisease)
                .distinct()
                .collect(Collectors.toList());

        // 物理删除今日已有记录（避免逻辑删除导致唯一键冲突）
        dailyLessonMapper.physicalDeleteByUserAndDate(userId, today.toString());

        // 新建并重新生成（排除当天已生成过的主题）
        DailyLesson placeholder = new DailyLesson();
        placeholder.setUserId(userId);
        placeholder.setLessonDate(today);
        placeholder.setIsGenerated(0);
        dailyLessonMapper.insert(placeholder);

        return doGenerateAndSave(placeholder, user, excludeDiseases);
    }

    @Override
    public List<DailyLessonDTO> getLessonHistory(Long userId, int page, int size) {
        LambdaQueryWrapper<DailyLesson> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyLesson::getUserId, userId)
                    .eq(DailyLesson::getIsGenerated, 1)
                    .orderByDesc(DailyLesson::getLessonDate);

        Page<DailyLesson> pageParam = new Page<>(page, size);
        Page<DailyLesson> result = dailyLessonMapper.selectPage(pageParam, queryWrapper);

        return result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void generateDailyLessons() {
        LocalDate today = LocalDate.now();
        logger.info("=== 开始批量预生成今日科普，日期: {} ===", today);

        // 查询所有有慢病史的用户
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.isNotNull(SysUser::getChronicDiseases)
                 .ne(SysUser::getChronicDiseases, "")
                 .eq(SysUser::getDeleted, 0);
        List<SysUser> users = userMapper.selectList(userQuery);

        if (users.isEmpty()) {
            logger.info("没有需要生成科普的用户");
            return;
        }

        logger.info("找到 {} 个有慢病史的用户", users.size());

        int generatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (SysUser user : users) {
            try {
                Long dbUserId = user.getId();

                // 检查是否已生成
                LambdaQueryWrapper<DailyLesson> existQuery = new LambdaQueryWrapper<>();
                existQuery.eq(DailyLesson::getUserId, dbUserId)
                         .eq(DailyLesson::getLessonDate, today)
                         .eq(DailyLesson::getIsGenerated, 1);
                if (dailyLessonMapper.selectCount(existQuery) > 0) {
                    skippedCount++;
                    continue;
                }

                // 解析慢病并选择一个
                List<String> diseases = parseDiseases(user.getChronicDiseases());
                if (diseases.isEmpty()) {
                    skippedCount++;
                    continue;
                }
                String selected = pickDiseaseForToday(diseases, Collections.emptyList());

                // 插入占位行
                DailyLesson placeholder = new DailyLesson();
                placeholder.setUserId(dbUserId);
                placeholder.setLessonDate(today);
                placeholder.setIsGenerated(0);
                dailyLessonMapper.insert(placeholder);

                // 调用AI生成
                Map<String, String> result = deepSeekService.generateDiseaseScienceLesson(
                        selected, user.getAge(), user.getGender());

                if (result != null && result.containsKey("title") && result.containsKey("content")) {
                    placeholder.setChronicDisease(selected);
                    placeholder.setTitle(result.get("title"));
                    placeholder.setContent(result.get("content"));
                    placeholder.setIsGenerated(1);
                    dailyLessonMapper.updateById(placeholder);
                    generatedCount++;
                    logger.debug("预生成成功 - userId: {}, 疾病: {}", dbUserId, selected);
                } else {
                    placeholder.setErrorMsg("定时任务：AI返回结果为空");
                    dailyLessonMapper.updateById(placeholder);
                    failedCount++;
                }
            } catch (Exception e) {
                logger.error("预生成科普失败 - userId: {}", user.getId(), e);
                failedCount++;
            }

            // 调用间隔200ms，避免API限流
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("=== 今日一课预生成完成 === 成功: {}, 跳过: {}, 失败: {}", generatedCount, skippedCount, failedCount);
    }
}
