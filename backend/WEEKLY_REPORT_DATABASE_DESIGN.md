# AI用药周报 - 数据库表设计文档

## 表名：medication_weekly_report

### 表说明
存储用户每周用药情况的统计报告和AI建议，支持历史查询和趋势分析。

---

## 字段设计

### 基础信息

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | bigint | PK, AUTO_INCREMENT | 主键ID |
| report_id | varchar(64) | UNIQUE, NOT NULL | 报告唯一标识（UUID） |
| user_id | bigint | NOT NULL, INDEX | 用户ID（关联sys_user.user_id） |
| start_date | date | NOT NULL | 周报起始日期 |
| end_date | date | NOT NULL | 周报结束日期 |

### JSON数据字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| statistics_json | text | 总体统计数据JSON |
| ai_summary | text | AI生成的用药总结和建议 |
| full_report_text | longtext | 完整报告文本（用于截图展示） |
| missed_drugs_json | text | 漏服药品列表JSON数组 |
| daily_summaries_json | longtext | 每日汇总详情JSON数组 |

### 关键指标冗余字段（便于快速查询和统计）

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| total_plans | int | 0 | 总计划数 |
| taken_count | int | 0 | 已服用数 |
| missed_count | int | 0 | 漏服数 |
| skipped_count | int | 0 | 跳过数 |
| compliance_rate | decimal(5,2) | 0.00 | 按时服药率（%） |
| drug_variety_count | int | 0 | 涉及药品种类数 |

### 时段分析

| 字段名 | 类型 | 说明 |
|--------|------|------|
| best_time_slot | varchar(50) | 表现最好的时段（如"早上"） |
| needs_improvement_time_slot | varchar(50) | 需要改进的时段（如"晚上"） |

### 时间戳

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| generated_at | datetime | CURRENT_TIMESTAMP | 报告生成时间 |
| created_at | datetime | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | datetime | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

## 索引设计

```sql
PRIMARY KEY (`id`)
UNIQUE KEY `uk_report_id` (`report_id`)
INDEX `idx_user_date` (`user_id`, `start_date`, `end_date`)
INDEX `idx_generated_at` (`generated_at`)
INDEX `idx_compliance_rate` (`compliance_rate`)
```

### 索引说明

1. **uk_report_id**: 保证报告唯一性，支持通过report_id快速查询
2. **idx_user_date**: 复合索引，支持按用户和日期范围查询历史周报
3. **idx_generated_at**: 支持按生成时间排序，快速获取最新周报
4. **idx_compliance_rate**: 支持合规率统计分析（如查询低合规率用户）

---

## JSON字段结构示例

### statistics_json
```json
{
  "totalPlans": 28,
  "takenCount": 24,
  "missedCount": 3,
  "skippedCount": 1,
  "pendingCount": 0,
  "complianceRate": 89.29,
  "drugVarietyCount": 4
}
```

### daily_summaries_json
```json
[
  {
    "date": "2026-06-17",
    "dayOfWeek": "周三",
    "totalPlans": 4,
    "takenCount": 4,
    "missedCount": 0,
    "complianceRate": 100.0,
    "drugs": ["阿司匹林", "二甲双胍"]
  }
]
```

### missed_drugs_json
```json
["降压药", "维生素C"]
```

---

## 设计思路

### 1. 为什么使用JSON字段？

**优点：**
- ✅ 灵活扩展：新增统计维度无需修改表结构
- ✅ 减少字段数量：避免大量稀疏列
- ✅ 保持数据结构完整性：嵌套数据直接存储

**缺点：**
- ❌ 查询性能略低（但已通过冗余字段优化）
- ❌ 无法直接使用SQL聚合函数

### 2. 为什么添加冗余字段？

**关键指标冗余**（total_plans, compliance_rate等）的目的：
- 🚀 快速查询：无需解析JSON即可获取核心指标
- 📊 统计分析：支持SQL聚合操作（AVG、SUM等）
- 🔍 索引优化：可以对冗余字段建立索引

**空间换时间**的典型应用场景。

### 3. 为什么不设计外键？

**考虑因素：**
- 周报是历史记录，即使用户删除也不应级联删除
- 减少数据库约束，提高写入性能
- 应用层保证数据一致性

---

## 常见查询场景

### 1. 查询用户最新周报
```sql
SELECT * FROM medication_weekly_report
WHERE user_id = ?
ORDER BY generated_at DESC
LIMIT 1;
```

### 2. 查询用户某月所有周报
```sql
SELECT * FROM medication_weekly_report
WHERE user_id = ?
  AND start_date >= '2026-06-01'
  AND end_date <= '2026-06-30'
ORDER BY start_date ASC;
```

### 3. 统计用户平均合规率
```sql
SELECT AVG(compliance_rate) as avg_rate
FROM medication_weekly_report
WHERE user_id = ?;
```

### 4. 查找低合规率用户（用于干预）
```sql
SELECT DISTINCT user_id, AVG(compliance_rate) as avg_rate
FROM medication_weekly_report
GROUP BY user_id
HAVING avg_rate < 60;
```

### 5. 分析时段表现
```sql
SELECT 
  best_time_slot,
  COUNT(*) as count,
  AVG(compliance_rate) as avg_rate
FROM medication_weekly_report
GROUP BY best_time_slot
ORDER BY count DESC;
```

---

## 数据生命周期管理

### 建议策略

1. **保留周期**: 建议保留最近1年的周报数据
2. **归档策略**: 超过1年的数据可归档到历史表
3. **清理脚本**:
```sql
-- 删除超过1年的周报（谨慎执行）
DELETE FROM medication_weekly_report
WHERE generated_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

## 性能优化建议

### 1. 分区表（可选）
如果数据量大（>100万条），可以考虑按月分区：
```sql
ALTER TABLE medication_weekly_report
PARTITION BY RANGE (YEAR(start_date) * 100 + MONTH(start_date)) (
  PARTITION p202601 VALUES LESS THAN (202602),
  PARTITION p202602 VALUES LESS THAN (202603),
  ...
);
```

### 2. 缓存热点数据
- 用户最新周报可以缓存到Redis（TTL: 1小时）
- 减少数据库查询压力

### 3. 批量插入优化
如果需要批量生成周报，使用批量插入：
```java
// MyBatis Plus批量插入
weeklyReportMapper.insertBatch(reportList);
```

---

## 扩展性考虑

### 未来可能新增的字段

1. **用户反馈**
   - `user_rating` tinyint - 用户对报告的评分（1-5星）
   - `user_feedback` text - 用户反馈意见

2. **更详细的分析**
   - `trend_analysis_json` text - 趋势分析数据
   - `comparison_json` text - 与上周/上月对比数据

3. **分享功能**
   - `share_count` int - 分享次数
   - `last_shared_at` datetime - 最后分享时间

**由于使用了JSON字段，新增这些维度只需在应用层处理，无需修改表结构！**

---

## 相关实体类和Mapper

- **Entity**: [MedicationWeeklyReport.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/entity/MedicationWeeklyReport.java)
- **Mapper接口**: [MedicationWeeklyReportMapper.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/mapper/MedicationWeeklyReportMapper.java)
- **Mapper XML**: [MedicationWeeklyReportMapper.xml](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/resources/mapper/MedicationWeeklyReportMapper.xml)
- **SQL脚本**: [init_weekly_report_table.sql](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/resources/init_weekly_report_table.sql)

---

## 执行SQL脚本

```bash
# MySQL命令行
mysql -u root -p elderly_medication < init_weekly_report_table.sql

# 或在MySQL客户端中
source /path/to/init_weekly_report_table.sql;
```

执行后会：
1. 创建medication_weekly_report表
2. 验证表结构
3. 显示创建成功提示
