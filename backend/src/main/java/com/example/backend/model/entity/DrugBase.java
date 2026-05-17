package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药品基础库实体类
 * 对应数据库表：drug_base
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@TableName("drug_base")
public class DrugBase {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 国药准字
     * 国家药品监督管理局批准的药品批准文号，唯一标识
     */
    @TableField("approval_number")
    private String approvalNumber;

    /**
     * 通用名（化学名）
     * 药品的标准名称，如"硝苯地平"
     */
    @TableField("generic_name")
    private String genericName;

    /**
     * 商品名
     * 如"开博通"，药品的市场销售名称
     */
    @TableField("trade_name")
    private String tradeName;

    /**
     * 俗名/别名
     * 如"降压0号"，用户常用的非标准名称
     */
    @TableField("common_name")
    private String commonName;

    /**
     * 规格
     * 如"5mg*30片"，药品的具体规格信息
     */
    @TableField("specification")
    private String specification;

    /**
     * 生产厂家
     * 药品的生产企业名称
     */
    @TableField("manufacturer")
    private String manufacturer;

    /**
     * 药品分类
     * 处方药/非处方药/保健品
     */
    @TableField("category")
    private String category;

    /**
     * 药品说明原文
     * 包含药品的详细说明信息
     */
    @TableField("description")
    private String description;

    /**
     * 药品标准图片
     * 药品包装或说明书的图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 录入时间
     * 记录创建时自动生成
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
