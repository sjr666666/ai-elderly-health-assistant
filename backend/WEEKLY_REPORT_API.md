# AI用药周报 - 后端接口文档

## 概述
AI用药周报功能可以生成用户最近7天（或自定义时间段）的用药情况统计报告，包含数据统计、AI总结和建议，参考冲突检测报告的截图下载实现思路。

## 核心功能

### 1. 数据统计
- 总体统计：总计划数、已服用数、漏服数、跳过数、按时服药率
- 每日详情：每天的用药情况和合规率
- 时段分析：找出表现最好和需要改进的时段
- 漏服药品列表

### 2. AI智能总结
- 基于用药数据自动生成专业总结
- 针对漏服情况给出建议
- 鼓励性话语提升用户依从性

### 3. 完整报告文本
- 格式化的文本报告，可用于前端截图展示
- 类似冲突检测报告的截图下载功能

## API接口

### 接口1：生成用药周报
**URL**: `POST /api/weekly-report/generate`

**请求参数**:
```
userId: Long (必填) - 用户ID
startDate: String (可选) - 起始日期，格式yyyy-MM-dd，默认7天前
endDate: String (可选) - 结束日期，格式yyyy-MM-dd，默认今天
```

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/weekly-report/generate?userId=123456&startDate=2026-06-17&endDate=2026-06-24"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "用药周报生成成功",
  "data": {
    "reportId": "uuid-string",
    "generatedAt": "2026-06-24T10:30:00",
    "userId": 123456,
    "startDate": "2026-06-17",
    "endDate": "2026-06-24",
    "statistics": {
      "totalPlans": 28,
      "takenCount": 24,
      "missedCount": 3,
      "skippedCount": 1,
      "pendingCount": 0,
      "complianceRate": 89.29,
      "drugVarietyCount": 4
    },
    "dailySummaries": [
      {
        "date": "2026-06-17",
        "dayOfWeek": "周三",
        "totalPlans": 4,
        "takenCount": 4,
        "missedCount": 0,
        "complianceRate": 100.0,
        "drugs": ["阿司匹林", "二甲双胍"]
      }
    ],
    "aiSummary": "【优秀】本周用药依从性非常好！继续保持规律服药的习惯。\n\n坚持规律服药是控制病情的关键，祝您健康！",
    "missedDrugs": ["降压药"],
    "bestTimeSlot": "早上",
    "needsImprovementTimeSlot": "晚上",
    "fullReportText": "完整的格式化报告文本..."
  }
}
```

### 接口2：获取最新周报（快捷接口）
**URL**: `GET /api/weekly-report/latest`

**请求参数**:
```
userId: Long (必填) - 用户ID
```

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/weekly-report/latest?userId=123456"
```

**说明**: 自动获取最近7天的用药周报

## 技术实现

### 文件结构
```
backend/src/main/java/com/example/backend/
├── model/dto/
│   ├── WeeklyReportRequest.java      # 请求DTO
│   └── WeeklyReportResponse.java     # 响应DTO
├── service/
│   └── WeeklyReportService.java      # Service接口
├── service/impl/
│   └── WeeklyReportServiceImpl.java  # Service实现（核心逻辑）
└── controller/
    └── WeeklyReportController.java   # Controller
```

### 核心逻辑流程

1. **数据查询**
   - 查询指定时间范围内的所有用药计划
   - 查询用药记录（medication_log）
   - 获取药品基础信息

2. **数据统计**
   - 计算总体统计数据
   - 按日期分组统计每日情况
   - 计算各时段的服药率

3. **AI总结生成**
   - 构建提示词（包含统计数据、漏服情况等）
   - 调用DeepSeek API生成智能总结
   - 如果AI调用失败，使用默认总结模板

4. **报告文本生成**
   - 格式化完整报告文本
   - 包含统计、分析、建议等所有内容
   - 用于前端截图展示

### 参考冲突检测报告实现

与冲突检测报告的截图功能类似：
- 后端生成完整的`fullReportText`字段
- 前端可以使用html2canvas对报告容器截图
- 支持下载为图片分享给家属或医生

## 下一步工作

### 数据库（待实现）
可以考虑添加周报历史记录表：
```sql
CREATE TABLE medication_weekly_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    report_id VARCHAR(64) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    statistics_json TEXT,
    ai_summary TEXT,
    full_report_text TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, start_date)
);
```

### 前端（待实现）
1. 创建周报展示页面
2. 使用html2canvas实现截图功能
3. 添加分享按钮（保存图片到相册）
4. 图表可视化（折线图显示每日合规率趋势）

## 注意事项

1. **性能优化**: 如果用户数据量大，考虑添加缓存机制
2. **AI调用**: 当前版本AI总结使用默认模板，后续可接入DeepSeek通用对话API
3. **日期范围**: 建议限制最大查询范围为30天，避免性能问题
4. **空数据处理**: 已处理无用药计划的情况，返回友好提示

## 测试建议

### 单元测试
```java
@Test
public void testGenerateWeeklyReport() {
    WeeklyReportRequest request = WeeklyReportRequest.builder()
        .userId(123456L)
        .build();
    
    WeeklyReportResponse report = weeklyReportService.generateWeeklyReport(request);
    
    assertNotNull(report);
    assertNotNull(report.getStatistics());
    assertNotNull(report.getAiSummary());
}
```

### 集成测试
1. 准备测试数据（用药计划、用药记录）
2. 调用API接口
3. 验证返回数据的准确性
4. 测试边界情况（无数据、部分漏服等）
