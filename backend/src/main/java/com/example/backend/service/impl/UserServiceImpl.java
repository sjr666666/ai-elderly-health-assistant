package com.example.backend.service.impl;

import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.config.JwtUtils;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.UserLoginRequest;
import com.example.backend.model.dto.UserLoginResponse;
import com.example.backend.model.dto.UserProfileResponse;
import com.example.backend.model.dto.UserProfileUpdateRequest;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserRegisterResponse;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.UserService;
import com.example.backend.service.ElderNotificationService;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.model.entity.GuardianElderRelation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ElderNotificationService elderNotificationService;
    private final GuardianElderRelationMapper guardianElderRelationMapper;

    @Override
    public UserRegisterResponse register(UserRegisterRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        // 生成雪花算法ID作为userId
        user.setUserId(SnowflakeIdGenerator.getInstance().nextId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        user.setAllergyHistory(request.getAllergyHistory());
        user.setChronicDiseases(request.getChronicDiseases());
        user.setKidneyFunction(request.getKidneyFunction());
        user.setLiverFunction(request.getLiverFunction());
        user.setIsPregnant(request.getIsPregnant());
        user.setIsBreastfeeding(request.getIsBreastfeeding());
        user.setIsSmoking(request.getIsSmoking());
        user.setIsDrinking(request.getIsDrinking());
        user.setPhone(request.getPhone());
        // 根据请求设置角色，仅允许 elder 或 family
        String role = request.getRole();
        if (SysUser.Role.FAMILY.getCode().equals(role)) {
            user.setRole(SysUser.Role.FAMILY.getCode());
        } else {
            user.setRole(SysUser.Role.ELDER.getCode());
        }

        userMapper.insert(user);

        return UserRegisterResponse.builder()
                .userId(String.valueOf(user.getUserId()))  // 转换为 String
                .username(user.getUsername())
                .password(request.getPassword())
                .build();
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return null;
        }

        // 生成JWT令牌
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        logger.info("用户登录成功 - userId: {}, username: {}, role: {}", user.getId(), user.getUsername(), user.getRole());

        return UserLoginResponse.builder()
                .id(user.getId())  // 添加数据库主键ID
                .userId(String.valueOf(user.getUserId()))  // 转换为 String
                .username(user.getUsername())
                .realName(user.getRealName())
                .age(user.getAge())
                .gender(user.getGender())
                .height(user.getHeight())
                .weight(user.getWeight())
                .allergyHistory(user.getAllergyHistory())
                .chronicDiseases(user.getChronicDiseases())
                .kidneyFunction(user.getKidneyFunction())
                .liverFunction(user.getLiverFunction())
                .isPregnant(user.getIsPregnant())
                .isBreastfeeding(user.getIsBreastfeeding())
                .isSmoking(user.getIsSmoking())
                .isDrinking(user.getIsDrinking())
                .role(user.getRole())
                .token(token)  // 返回JWT令牌
                .build();
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        // userId 为数据库主键 id（由 SecurityContext 提供）
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return UserProfileResponse.builder()
                .userId(String.valueOf(user.getUserId()))  // 转换为 String
                .realName(user.getRealName())
                .age(user.getAge())
                .gender(user.getGender())
                .height(user.getHeight())
                .weight(user.getWeight())
                .allergyHistory(user.getAllergyHistory())
                .chronicDiseases(user.getChronicDiseases())
                .kidneyFunction(user.getKidneyFunction())
                .liverFunction(user.getLiverFunction())
                .isPregnant(user.getIsPregnant())
                .isBreastfeeding(user.getIsBreastfeeding())
                .isSmoking(user.getIsSmoking())
                .isDrinking(user.getIsDrinking())
                .role(user.getRole())
                .phone(user.getPhone())
                .build();
    }

    @Override
    public void updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        logger.info("更新用户档案信息 - userId: {}, realName: {}, age: {}, gender: {}, height: {}, weight: {}, "
                        + "allergyHistory: {}, chronicDiseases: {}, kidneyFunction: {}, liverFunction: {}, "
                        + "isPregnant: {}, isBreastfeeding: {}, isSmoking: {}, isDrinking: {}",
                userId, request.getRealName(), request.getAge(), request.getGender(),
                request.getHeight(), request.getWeight(),
                request.getAllergyHistory(), request.getChronicDiseases(),
                request.getKidneyFunction(), request.getLiverFunction(),
                request.getIsPregnant(), request.getIsBreastfeeding(),
                request.getIsSmoking(), request.getIsDrinking());

        // userId 为数据库主键 id（由 SecurityContext 提供）
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            logger.error("用户不存在 - userId: {}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 更新称呼（realName）
        if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
            user.setRealName(request.getRealName().trim());
        }
        // 更新年龄
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        // 更新性别
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            user.setGender(request.getGender().trim().toLowerCase());
        }
        // 更新身高
        if (request.getHeight() != null) {
            user.setHeight(request.getHeight());
        }
        // 更新体重
        if (request.getWeight() != null) {
            user.setWeight(request.getWeight());
        }
        // 更新过敏史
        if (request.getAllergyHistory() != null) {
            user.setAllergyHistory(request.getAllergyHistory());
        }
        // 更新慢性病史（允许空字符串清空）
        if (request.getChronicDiseases() != null) {
            user.setChronicDiseases(request.getChronicDiseases().trim().isEmpty() ? null : request.getChronicDiseases());
        }
        // 更新肾功能状态
        if (request.getKidneyFunction() != null && !request.getKidneyFunction().trim().isEmpty()) {
            user.setKidneyFunction(request.getKidneyFunction().trim().toLowerCase());
        }
        // 更新肝功能状态
        if (request.getLiverFunction() != null && !request.getLiverFunction().trim().isEmpty()) {
            user.setLiverFunction(request.getLiverFunction().trim().toLowerCase());
        }
        // 更新孕期状态
        if (request.getIsPregnant() != null) {
            user.setIsPregnant(request.getIsPregnant());
        }
        // 更新哺乳期状态
        if (request.getIsBreastfeeding() != null) {
            user.setIsBreastfeeding(request.getIsBreastfeeding());
        }
        // 更新吸烟状态
        if (request.getIsSmoking() != null) {
            user.setIsSmoking(request.getIsSmoking());
        }
        // 更新饮酒状态
        if (request.getIsDrinking() != null) {
            user.setIsDrinking(request.getIsDrinking());
        }
        // 更新联系电话
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String oldPhone = user.getPhone();
            user.setPhone(request.getPhone().trim());
            // 家属修改电话时，通知绑定的老人
            if ("family".equals(user.getRole()) && !request.getPhone().trim().equals(oldPhone)) {
                notifyEldersPhoneChanged(user, oldPhone, request.getPhone().trim());
            }
        }

        int result = userMapper.updateById(user);
        logger.info("用户档案更新结果 - userId: {}, 影响行数: {}", userId, result);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // userId 为数据库主键 id（由 SecurityContext 提供）
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        logger.info("用户密码修改成功 - userId: {}", userId);
    }

    /**
     * 家属电话变更时通知绑定的老人
     */
    private void notifyEldersPhoneChanged(SysUser guardian, String oldPhone, String newPhone) {
        try {
            LambdaQueryWrapper<GuardianElderRelation> query = new LambdaQueryWrapper<>();
            query.eq(GuardianElderRelation::getGuardianId, guardian.getId())
                    .eq(GuardianElderRelation::getStatus, "active");
            List<GuardianElderRelation> bindings = guardianElderRelationMapper.selectList(query);
            for (GuardianElderRelation binding : bindings) {
                String extraData = String.format(
                        "{\"guardianId\":%d,\"guardianName\":\"%s\",\"oldPhone\":\"%s\",\"newPhone\":\"%s\",\"relationship\":\"%s\"}",
                        guardian.getId(), guardian.getRealName(),
                        oldPhone != null ? oldPhone : "",
                        newPhone,
                        binding.getRelationType() != null ? binding.getRelationType() : "");
                elderNotificationService.createNotification(
                        binding.getElderId(),
                        "phone_update",
                        "联系电话变更",
                        guardian.getRealName() + "的联系电话已变更为" + newPhone + "，是否更新紧急联系人电话？",
                        extraData
                );
            }
        } catch (Exception e) {
            logger.warn("通知老人电话变更失败 - guardianId: {}, error: {}", guardian.getUserId(), e.getMessage());
        }
    }
}