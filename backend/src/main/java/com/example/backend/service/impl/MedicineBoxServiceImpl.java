package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserMedicineBoxMapper;
import com.example.backend.model.dto.AddMedicineRequest;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.UpdateMedicineRequest;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.SysUser;
import com.example.backend.model.entity.UserMedicineBox;
import com.example.backend.service.MedicineBoxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 药箱服务实现类
 */
@Service
public class MedicineBoxServiceImpl implements MedicineBoxService {

    private static final Logger logger = LoggerFactory.getLogger(MedicineBoxServiceImpl.class);

    private final UserMedicineBoxMapper userMedicineBoxMapper;
    private final UserMapper userMapper;
    private final MedicationPlanMapper medicationPlanMapper;

    @Autowired
    public MedicineBoxServiceImpl(UserMedicineBoxMapper userMedicineBoxMapper, 
                                 UserMapper userMapper,
                                 MedicationPlanMapper medicationPlanMapper) {
        this.userMedicineBoxMapper = userMedicineBoxMapper;
        this.userMapper = userMapper;
        this.medicationPlanMapper = medicationPlanMapper;
    }

    @Override
    public void addMedicineToBox(Long userId, AddMedicineRequest request) {
        logger.info("添加药品到药箱 - userId (雪花算法ID): {}, drugId: {}, dosage: {}, frequency: {}",
                userId, request.getDrugId(), request.getDosage(), request.getFrequency());

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserId, userId);
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
            existWrapper.eq(UserMedicineBox::getStatus, "active");
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
        medicineBox.setExpiryDate(LocalDate.parse(request.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));

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
    }

    @Override
    public List<MedicineBoxResponse> getMedicineBoxList(Long userId, String status) {
        logger.info("获取药箱列表 - userId (雪花算法ID): {}, status: {}", userId, status);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        
        // 默认状态为 active
        if (status == null || status.isEmpty()) {
            status = "active";
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
        queryWrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        
        // 默认状态为 active
        if (status == null || status.isEmpty()) {
            status = "active";
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
        queryWrapper.eq(SysUser::getUserId, userId);
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
        queryWrapper.eq(SysUser::getUserId, userId);
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
}
