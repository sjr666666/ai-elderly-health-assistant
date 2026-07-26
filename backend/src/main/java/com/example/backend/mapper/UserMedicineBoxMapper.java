package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.entity.UserMedicineBox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
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

    /**
     * 原子扣减库存（remaining_quantity - amount，最小为0）
     *
     * @param boxItemId 药箱条目ID
     * @param amount 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE user_medicine_box SET remaining_quantity = GREATEST(0, remaining_quantity - #{amount}) WHERE id = #{boxItemId} AND remaining_quantity IS NOT NULL")
    int deductInventory(@Param("boxItemId") Long boxItemId, @Param("amount") BigDecimal amount);

    /**
     * 原子恢复库存（remaining_quantity + amount）
     *
     * @param boxItemId 药箱条目ID
     * @param amount 恢复数量
     * @return 影响行数
     */
    @Update("UPDATE user_medicine_box SET remaining_quantity = remaining_quantity + #{amount} WHERE id = #{boxItemId} AND remaining_quantity IS NOT NULL")
    int restoreInventory(@Param("boxItemId") Long boxItemId, @Param("amount") BigDecimal amount);
}
