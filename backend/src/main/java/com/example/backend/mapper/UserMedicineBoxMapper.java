package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.entity.UserMedicineBox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户药箱 Mapper 接口
 */
@Mapper
public interface UserMedicineBoxMapper extends BaseMapper<UserMedicineBox> {

    /**
     * 获取药箱列表（关联药品基础库查询药品名称）
     *
     * @param userId 用户自增主键ID
     * @param status 状态过滤
     * @return 药箱列表
     */
    List<MedicineBoxResponse> selectMedicineBoxList(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 搜索药箱中药品（支持模糊匹配药品名称、用量或备注）
     *
     * @param userId 用户自增主键ID
     * @param keyword 搜索关键词
     * @param status 状态过滤
     * @return 匹配的药箱条目列表
     */
    List<MedicineBoxResponse> searchMedicineBox(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("status") String status);
}
