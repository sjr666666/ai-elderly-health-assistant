package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserMedicineBoxMapper;
import com.example.backend.model.dto.AddMedicineRequest;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.MedicineShortageWarningDTO;
import com.example.backend.model.dto.UpdateMedicineRequest;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.SysUser;
import com.example.backend.model.entity.UserMedicineBox;
import com.example.backend.model.enums.Severity;
import com.example.backend.service.MedicineBoxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 药箱服务实现类
 */
@Service
public class MedicineBoxServiceImpl implements MedicineBoxService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineBoxServiceImpl.class);

    private final UserMedicineBoxMapper userMedicineBoxMapper;
    private final UserMapper userMapper;
    private final MedicationPlanMapper medicationPlanMapper;
    private final DrugBaseMapper drugBaseMapper;

    @Autowired
    public MedicineBoxServiceImpl(UserMedicineBoxMapper userMedicineBoxMapper, 
                                 UserMapper userMapper,
                                 MedicationPlanMapper medicationPlanMapper,
                                 DrugBaseMapper drugBaseMapper) {
        this.userMedicineBoxMapper = userMedicineBoxMapper;
        this.userMapper = userMapper;
        this.medicationPlanMapper = medicationPlanMapper;
        this.drugBaseMapper = drugBaseMapper;
    }

    @Override
    public void addMedicineToBox(Long userId, AddMedicineRequest request) {
        logger.info("添加药品到药箱 - userId (雪花算法ID): {}, drugId: {}, dosage: {}, frequency: {}",
                userId, request.getDrugId(), request.getDosage(), request.getFrequency());

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        logger.info("用户 ID 映射 - 雪花算法ID: {}, 自增主键ID: {}", userId, actualUserId);

        // 检查是否已存在相同的药品（同名药品在active状态）
        // 优先使用 name 字段，如果不存在则使用 drugName 字段
        String drugNameToCheck = request.getName();
        if (drugNameToCheck == null || drugNameToCheck.isEmpty()) {
            drugNameToCheck = request.getDrugName();
        }
        
        if (drugNameToCheck != null && !drugNameToCheck.isEmpty()) {
            // 检查AI药品重复
            LambdaQueryWrapper<UserMedicineBox> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(UserMedicineBox::getUserId, actualUserId);
            existWrapper.eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode());
            existWrapper.like(UserMedicineBox::getNote, "{AI药品:" + drugNameToCheck + "}");
            Long existCount = userMedicineBoxMapper.selectCount(existWrapper);
            if (existCount > 0) {
                logger.error("药品已存在 - userId: {}, drugName: {}", userId, drugNameToCheck);
                throw new RuntimeException("该药品已在药箱中，请勿重复添加");
            }
        }

        UserMedicineBox medicineBox = new UserMedicineBox();
        medicineBox.setUserId(actualUserId);  // 使用自增主键 ID
        medicineBox.setDrugId(request.getDrugId());
        medicineBox.setDosage(request.getDosage());
        medicineBox.setFrequency(request.getFrequency());

        // 处理开始日期，默认为今天
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            medicineBox.setStartDate(LocalDate.parse(request.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            medicineBox.setStartDate(LocalDate.now());
        }

        // 处理结束日期
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            medicineBox.setEndDate(LocalDate.parse(request.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        // 处理有效期
        LocalDate expiryDate = LocalDate.parse(request.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        medicineBox.setExpiryDate(expiryDate);

        // 处理总数量和剩余数量
        if (request.getTotalQuantity() != null) {
            medicineBox.setTotalQuantity(request.getTotalQuantity());
            medicineBox.setRemainingQuantity(request.getTotalQuantity()); // 初始时剩余数量等于总数量
        }

        // 处理备注
        String note = request.getNote();
        // 如果药品ID为空但有药品名称，说明是AI搜索或手动输入的药品，存储药品名称到备注
        // 优先使用 name 字段，如果不存在则使用 drugName 字段
        String drugNameToStore = request.getName();
        if (drugNameToStore == null || drugNameToStore.isEmpty()) {
            drugNameToStore = request.getDrugName();
        }
        
        if ((request.getDrugId() == null || request.getDrugId() == 0) 
                && drugNameToStore != null && !drugNameToStore.isEmpty()) {
            note = "{AI药品:" + drugNameToStore + "}" + (note != null ? note : "");
        }
        medicineBox.setNote(note);

        // 处理状态，默认为 active
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            medicineBox.setStatus(request.getStatus());
        } else {
            medicineBox.setStatus(UserMedicineBox.Status.ACTIVE.getCode());
        }

        int result = userMedicineBoxMapper.insert(medicineBox);
        logger.info("药品添加结果 - userId: {}, drugId: {}, 影响行数: {}", userId, request.getDrugId(), result);

        if (result <= 0) {
            throw new RuntimeException("药品添加失败");
        }

        // 不再自动检查过期并修改status，允许用户手动丢弃过期药品
        // 过期药品仍然保留在药箱中，显示红色边框，用户可以点击"我已丢弃"按钮进行处理
    }

    @Override
    public List<MedicineBoxResponse> getMedicineBoxList(Long userId, String status) {
        logger.info("获取药箱列表 - userId (雪花算法ID): {}, status: {}", userId, status);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        
        // 默认状态为 active
        if (status == null || status.isEmpty()) {
            status = UserMedicineBox.Status.ACTIVE.getCode();
        }

        List<MedicineBoxResponse> boxList = userMedicineBoxMapper.selectMedicineBoxList(actualUserId, status);
        logger.info("查询到药箱列表数量: {}", boxList.size());
        return boxList;
    }

    @Override
    public List<MedicineBoxResponse> searchMedicineBox(Long userId, String keyword, String status) {
        logger.info("搜索药箱中药品 - userId (雪花算法ID): {}, keyword: {}, status: {}", userId, keyword, status);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        
        // 默认状态为 active
        if (status == null || status.isEmpty()) {
            status = UserMedicineBox.Status.ACTIVE.getCode();
        }

        List<MedicineBoxResponse> searchResults = userMedicineBoxMapper.searchMedicineBox(actualUserId, keyword, status);
        logger.info("搜索到药箱条目数量: {}", searchResults.size());
        return searchResults;
    }

    @Override
    public void updateMedicineBoxEntry(Long userId, Long boxId, UpdateMedicineRequest request) {
        logger.info("修改药箱条目 - userId (雪花算法ID): {}, boxId: {}", userId, boxId);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();

        // 查询药箱条目，并进行越权校验
        UserMedicineBox medicineBox = userMedicineBoxMapper.selectById(boxId);
        if (medicineBox == null) {
            logger.error("药箱条目不存在 - boxId: {}", boxId);
            throw new RuntimeException("药箱条目不存在");
        }

        // 越权校验：确认该药箱条目属于当前用户
        if (!medicineBox.getUserId().equals(actualUserId)) {
            logger.error("越权操作 - userId: {}, boxId: {}", userId, boxId);
            throw new RuntimeException("无权修改该药箱条目");
        }

        // 只更新非空字段（部分更新）
        if (request.getDosage() != null && !request.getDosage().isEmpty()) {
            medicineBox.setDosage(request.getDosage());
        }
        if (request.getFrequency() != null && !request.getFrequency().isEmpty()) {
            medicineBox.setFrequency(request.getFrequency());
        }
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            medicineBox.setStartDate(LocalDate.parse(request.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            medicineBox.setEndDate(LocalDate.parse(request.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (request.getExpiryDate() != null && !request.getExpiryDate().isEmpty()) {
            medicineBox.setExpiryDate(LocalDate.parse(request.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (request.getTotalQuantity() != null) {
            medicineBox.setTotalQuantity(request.getTotalQuantity());
        }
        if (request.getRemainingQuantity() != null) {
            medicineBox.setRemainingQuantity(request.getRemainingQuantity());
        }
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            medicineBox.setNote(request.getNote());
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            medicineBox.setStatus(request.getStatus());
        }

        int result = userMedicineBoxMapper.updateById(medicineBox);
        logger.info("修改药箱条目结果 - boxId: {}, 影响行数: {}", boxId, result);

        if (result <= 0) {
            throw new RuntimeException("修改药箱条目失败");
        }
    }

    @Override
    public void deleteMedicineBoxEntry(Long userId, Long boxId) {
        logger.info("删除药箱条目 - userId (雪花算法ID): {}, boxId: {}", userId, boxId);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();

        // 查询药箱条目，并进行越权校验
        UserMedicineBox medicineBox = userMedicineBoxMapper.selectById(boxId);
        if (medicineBox == null) {
            logger.error("药箱条目不存在 - boxId: {}", boxId);
            throw new RuntimeException("药箱条目不存在");
        }

        // 越权校验：确认该药箱条目属于当前用户
        if (!medicineBox.getUserId().equals(actualUserId)) {
            logger.error("越权操作 - userId: {}, boxId: {}", userId, boxId);
            throw new RuntimeException("无权删除该药箱条目");
        }

        // 逻辑删除：将状态改为 stopped
        medicineBox.setStatus(UserMedicineBox.Status.STOPPED.getCode());
        int result = userMedicineBoxMapper.updateById(medicineBox);
        logger.info("删除药箱条目结果 - boxId: {}, 影响行数: {}", boxId, result);

        if (result <= 0) {
            throw new RuntimeException("删除药箱条目失败");
        }

        // 同时删除该药箱条目对应的用药计划（硬删除，彻底删除）
        LambdaQueryWrapper<MedicationPlan> planQueryWrapper = new LambdaQueryWrapper<>();
        planQueryWrapper.eq(MedicationPlan::getUserId, actualUserId)
                      .eq(MedicationPlan::getBoxItemId, boxId);
        int planResult = medicationPlanMapper.delete(planQueryWrapper);
        logger.info("删除药箱条目对应的用药计划 - boxId: {}, 删除数量: {}", boxId, planResult);
    }

    @Override
    public List<MedicineBoxResponse> getTodayExpiredMedicines(Long userId) {
        logger.info("获取所有已过期且未丢弃的药品列表 - userId (雪花算法ID): {}", userId);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        LocalDate today = LocalDate.now();

        // 查询条件：status=active 且 expiryDate < 今天（只查询未丢弃的药品）
        LambdaQueryWrapper<UserMedicineBox> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.eq(UserMedicineBox::getUserId, actualUserId)
                     .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode())
                     .lt(UserMedicineBox::getExpiryDate, today)
                     .orderByDesc(UserMedicineBox::getExpiryDate);

        List<UserMedicineBox> expiredMedicines = userMedicineBoxMapper.selectList(expiredWrapper);
        
        // 转换为 MedicineBoxResponse
        List<MedicineBoxResponse> todayExpiredList = expiredMedicines.stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
        
        logger.info("查询到已过期且未丢弃的药品数量: {}", todayExpiredList.size());
        return todayExpiredList;
    }

    /**
     * 将 UserMedicineBox 转换为 MedicineBoxResponse
     */
    private MedicineBoxResponse convertToResponse(UserMedicineBox box) {
        MedicineBoxResponse response = new MedicineBoxResponse();
        response.setBoxItemId(box.getId());
        response.setDrugId(box.getDrugId());
        
        // 提取药品名称：优先从数据库查询，其次从 note 字段提取
        String drugName = "未知药品";
        
        if (box.getDrugId() != null && box.getDrugId() > 0) {
            // 有 drugId，从数据库查询药品信息
            try {
                DrugBase drug = drugBaseMapper.selectById(box.getDrugId());
                if (drug != null) {
                    // 优先使用俗名/别名，其次使用通用名
                    if (drug.getCommonName() != null && !drug.getCommonName().isEmpty()) {
                        drugName = drug.getCommonName();
                    } else if (drug.getGenericName() != null && !drug.getGenericName().isEmpty()) {
                        drugName = drug.getGenericName();
                    } else if (drug.getTradeName() != null && !drug.getTradeName().isEmpty()) {
                        drugName = drug.getTradeName();
                    }
                    
                    // 设置其他药品信息
                    response.setGenericName(drug.getGenericName());
                    response.setTradeName(drug.getTradeName());
                    response.setCommonName(drug.getCommonName());
                    response.setSpecification(drug.getSpecification());
                }
            } catch (Exception e) {
                logger.warn("查询药品基础信息失败 - drugId: {}, error: {}", box.getDrugId(), e.getMessage());
            }
        }
        
        // 如果数据库查询失败或没有 drugId，尝试从 note 字段提取 AI 药品名称
        if ("未知药品".equals(drugName)) {
            String note = box.getNote();
            if (note != null && note.contains("{AI药品:")) {
                int start = note.indexOf("{AI药品:") + 9;
                int end = note.indexOf("}", start);
                if (end > start) {
                    drugName = note.substring(start, end);
                }
            }
        }
        
        response.setDrugName(drugName);
        
        // 如果还没有规格信息，设置为空字符串
        if (response.getSpecification() == null) {
            response.setSpecification("");
        }
        
        response.setDosage(box.getDosage());
        response.setFrequency(box.getFrequency());
        response.setStartDate(box.getStartDate() != null ? box.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        response.setEndDate(box.getEndDate() != null ? box.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        response.setExpiryDate(box.getExpiryDate() != null ? box.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        response.setTotalQuantity(box.getTotalQuantity());
        response.setRemainingQuantity(box.getRemainingQuantity());
        response.setNote(box.getNote());
        response.setStatus(box.getStatus());
        response.setCreatedAt(box.getCreatedAt() != null ? box.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        
        return response;
    }

    @Override
    public List<MedicineShortageWarningDTO> getShortageWarnings(Long userId) {
        logger.info("获取缺药预警 - userId (雪花算法ID): {}", userId);

        // 查询用户实际ID
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();

        // 查询所有活跃状态的药品
        LambdaQueryWrapper<UserMedicineBox> boxWrapper = new LambdaQueryWrapper<>();
        boxWrapper.eq(UserMedicineBox::getUserId, actualUserId)
                  .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode());
        List<UserMedicineBox> activeMedicines = userMedicineBoxMapper.selectList(boxWrapper);

        List<MedicineShortageWarningDTO> warnings = new ArrayList<>();

        for (UserMedicineBox box : activeMedicines) {
            try {
                MedicineShortageWarningDTO warning = calculateShortageWarning(box);
                // 只返回剩余天数小于7天的预警
                if (warning != null && warning.getRemainingDays() != null && warning.getRemainingDays() < 7) {
                    warnings.add(warning);
                }
            } catch (Exception e) {
                logger.warn("计算药品缺药预警失败 - boxId: {}, error: {}", box.getId(), e.getMessage());
            }
        }

        // 按剩余天数升序排列（最紧急的排前面）
        warnings.sort(Comparator.comparingInt(w -> w.getRemainingDays() != null ? w.getRemainingDays() : Integer.MAX_VALUE));

        logger.info("缺药预警数量: {}", warnings.size());
        return warnings;
    }

    /**
     * 计算单个药品的缺药预警
     * 算法：剩余天数 = 剩余数量 / (每次用量 × 每日服用次数)
     */
    private MedicineShortageWarningDTO calculateShortageWarning(UserMedicineBox box) {
        // 数据完整性校验
        if (box.getRemainingQuantity() == null || box.getRemainingQuantity() <= 0) {
            // 剩余为0或为空，直接返回已用尽预警
            return buildWarningDTO(box, 0.0, 0);
        }

        if (box.getDosage() == null || box.getDosage().isEmpty() ||
            box.getFrequency() == null || box.getFrequency().isEmpty()) {
            // 频率或剂量数据不完整，无法计算
            logger.warn("药品数据不完整，无法计算剩余天数 - boxId: {}, dosage: {}, frequency: {}",
                    box.getId(), box.getDosage(), box.getFrequency());
            return null;
        }

        // 解析每次用量（从字符串中提取数字）
        double dosagePerTime = parseDosage(box.getDosage());
        if (dosagePerTime <= 0) {
            logger.warn("无法解析每次用量 - boxId: {}, dosage: {}", box.getId(), box.getDosage());
            return null;
        }

        // 解析每日服用次数
        double timesPerDay = parseFrequency(box.getFrequency());
        if (timesPerDay <= 0) {
            logger.warn("无法解析服用频率 - boxId: {}, frequency: {}", box.getId(), box.getFrequency());
            return null;
        }

        // 计算每日消耗量
        double dailyConsumption = dosagePerTime * timesPerDay;

        // 计算剩余天数
        int remainingDays = (int) Math.floor(box.getRemainingQuantity() / dailyConsumption);

        return buildWarningDTO(box, dailyConsumption, remainingDays);
    }

    /**
     * 解析每次用量字符串，提取数字
     * 支持格式："1片"、"半片"、"0.5片"、"2粒"、"5ml" 等
     */
    private double parseDosage(String dosage) {
        if (dosage == null || dosage.isEmpty()) return 0;

        // 处理"半片"等中文表达
        if (dosage.contains("半")) {
            return 0.5;
        }

        // 提取数字（支持小数）
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(dosage);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return 0;
    }

    /**
     * 解析服用频率字符串，计算每日服用次数
     * 支持格式：
     * - "每日一次" → 1
     * - "每日两次"/"每日2次" → 2
     * - "每日三次"/"每日3次" → 3
     * - "一天两次"/"一天2次" → 2
     * - "早晚各一次" → 2
     * - "早中晚各一次" → 3
     * - "睡前" → 1
     * - "隔日一次" → 0.5
     * - "每周一次" → 1/7
     */
    private double parseFrequency(String frequency) {
        if (frequency == null || frequency.isEmpty()) return 0;

        // 中文数字映射
        String[] cnNumbers = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

        // 尝试匹配 "每日X次" / "一天X次" / "每天X次"
        Pattern dailyPattern = Pattern.compile("(?:每日|每天|一天)([一二三四五六七八九十\\d]+)次");
        Matcher dailyMatcher = dailyPattern.matcher(frequency);
        if (dailyMatcher.find()) {
            return parseCnOrDigitNumber(dailyMatcher.group(1), cnNumbers);
        }

        // 匹配 "X次/天" / "X次每天"
        Pattern perDayPattern = Pattern.compile("([一二三四五六七八九十\\d]+)次[/每]天");
        Matcher perDayMatcher = perDayPattern.matcher(frequency);
        if (perDayMatcher.find()) {
            return parseCnOrDigitNumber(perDayMatcher.group(1), cnNumbers);
        }

        // 匹配 "早中晚各一次" → 3次
        if (frequency.contains("早") && frequency.contains("中") && frequency.contains("晚")) {
            return 3;
        }

        // 匹配 "早晚各一次" → 2次
        if ((frequency.contains("早") || frequency.contains("晨")) && frequency.contains("晚")) {
            return 2;
        }

        // 匹配 "隔日一次" / "隔天一次" → 0.5次/天
        if (frequency.contains("隔日") || frequency.contains("隔天")) {
            return 0.5;
        }

        // 匹配 "每周一次" / "一周一次" → 1/7次/天
        if (frequency.contains("每周") || frequency.contains("一周")) {
            Pattern weekPattern = Pattern.compile("(?:每周|一周)([一二三四五六七八九十\\d]+)次");
            Matcher weekMatcher = weekPattern.matcher(frequency);
            if (weekMatcher.find()) {
                double times = parseCnOrDigitNumber(weekMatcher.group(1), cnNumbers);
                return times / 7.0;
            }
            return 1.0 / 7.0;
        }

        // 匹配 "睡前" / "饭后" 等单次描述 → 1次/天
        if (frequency.contains("睡前") || frequency.contains("饭后") || frequency.contains("饭前")) {
            // 检查是否有多个时间点
            int count = 0;
            if (frequency.contains("早") || frequency.contains("晨")) count++;
            if (frequency.contains("中") || frequency.contains("午")) count++;
            if (frequency.contains("晚")) count++;
            if (frequency.contains("睡前")) count++;
            if (frequency.contains("饭前") || frequency.contains("饭后")) count++;
            return Math.max(count, 1);
        }

        // 匹配纯数字+次，如 "2次" → 2
        Pattern simplePattern = Pattern.compile("([一二三四五六七八九十\\d]+)次");
        Matcher simpleMatcher = simplePattern.matcher(frequency);
        if (simpleMatcher.find()) {
            return parseCnOrDigitNumber(simpleMatcher.group(1), cnNumbers);
        }

        // 默认：无法识别的频率按每日1次处理
        logger.warn("无法识别频率格式，按每日1次处理 - frequency: {}", frequency);
        return 1;
    }

    /**
     * 解析中文或阿拉伯数字
     */
    private double parseCnOrDigitNumber(String str, String[] cnNumbers) {
        // 先尝试阿拉伯数字
        Pattern digitPattern = Pattern.compile("(\\d+)");
        Matcher digitMatcher = digitPattern.matcher(str);
        if (digitMatcher.find()) {
            return Double.parseDouble(digitMatcher.group(1));
        }

        // 中文数字
        for (int i = 1; i < cnNumbers.length; i++) {
            if (str.contains(cnNumbers[i])) {
                return i;
            }
        }

        return 1;
    }

    /**
     * 构建缺药预警DTO
     */
    private MedicineShortageWarningDTO buildWarningDTO(UserMedicineBox box, double dailyConsumption, int remainingDays) {
        // 获取药品名称
        String drugName = "未知药品";
        String specification = "";

        if (box.getDrugId() != null && box.getDrugId() > 0) {
            try {
                DrugBase drug = drugBaseMapper.selectById(box.getDrugId());
                if (drug != null) {
                    if (drug.getCommonName() != null && !drug.getCommonName().isEmpty()) {
                        drugName = drug.getCommonName();
                    } else if (drug.getGenericName() != null && !drug.getGenericName().isEmpty()) {
                        drugName = drug.getGenericName();
                    } else if (drug.getTradeName() != null && !drug.getTradeName().isEmpty()) {
                        drugName = drug.getTradeName();
                    }
                    specification = drug.getSpecification() != null ? drug.getSpecification() : "";
                }
            } catch (Exception e) {
                logger.warn("查询药品信息失败 - drugId: {}", box.getDrugId());
            }
        }

        // 从note字段提取AI药品名称
        if ("未知药品".equals(drugName) && box.getNote() != null && box.getNote().contains("{AI药品:")) {
            int start = box.getNote().indexOf("{AI药品:") + 9;
            int end = box.getNote().indexOf("}", start);
            if (end > start) {
                drugName = box.getNote().substring(start, end);
            }
        }

        // 确定预警级别
        String warningLevel;
        String warningLevelDesc;
        if (remainingDays <= 0) {
            warningLevel = Severity.CRITICAL.getCode();
            warningLevelDesc = "药品已用尽";
        } else if (remainingDays <= 3) {
            warningLevel = Severity.URGENT.getCode();
            warningLevelDesc = "药品即将用尽";
        } else {
            warningLevel = Severity.WARNING.getCode();
            warningLevelDesc = "药品余量不足";
        }

        return MedicineShortageWarningDTO.builder()
                .boxItemId(box.getId())
                .drugId(box.getDrugId())
                .drugName(drugName)
                .specification(specification)
                .dosage(box.getDosage())
                .frequency(box.getFrequency())
                .remainingQuantity(box.getRemainingQuantity())
                .dailyConsumption(Math.round(dailyConsumption * 100.0) / 100.0)
                .remainingDays(remainingDays)
                .warningLevel(warningLevel)
                .warningLevelDesc(warningLevelDesc)
                .build();
    }
}
