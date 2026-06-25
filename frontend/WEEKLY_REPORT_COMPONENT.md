# AI用药周报 - 前端组件使用文档

## 组件文件

- **组件**: [WeeklyReport.jsx](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/WeeklyReport.jsx)
- **样式**: [WeeklyReport.css](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/WeeklyReport.css)

---

## 功能特性

### ✅ 已实现功能

1. **数据展示**
   - 总体统计（总计划数、已服用、漏服、跳过、合规率）
   - 时段分析（最佳时段、需改进时段）
   - 漏服药品列表
   - 每日详情（日期、合规率进度条、药品列表）
   - AI智能建议

2. **交互功能**
   - 📷 截图保存：使用html2canvas生成PNG图片下载
   - 📋 复制文本：一键复制完整报告文本
   - 🔄 刷新数据：重新加载最新周报
   - 📅 历史记录：查看历史周报列表（预留接口）

3. **视觉效果**
   - 响应式设计（支持移动端）
   - 颜色编码（合规率不同等级显示不同颜色）
   - 渐变背景、圆角卡片、阴影效果
   - 加载动画、错误提示、空状态

4. **打印优化**
   - 隐藏操作按钮
   - 去除阴影，简化边框

---

## 使用方法

### 1. 在App.js中引入组件

```javascript
import WeeklyReport from './components/WeeklyReport';

// 在render函数中添加
const renderWeeklyReportTab = () => (
  <WeeklyReport userId={user?.userId} />
);
```

### 2. 添加导航标签

```javascript
// 在底部导航栏添加"周报"选项
<nav className="bottom-nav">
  <button 
    className={`nav-item ${activeTab === 'weekly-report' ? 'active' : ''}`}
    onClick={() => setActiveTab('weekly-report')}
  >
    <span className="nav-icon">📊</span>
    <span className="nav-label">周报</span>
  </button>
</nav>
```

### 3. 在主渲染逻辑中添加

```javascript
{activeTab === 'weekly-report' && renderWeeklyReportTab()}
```

---

## API调用

### 获取最新周报

**接口**: `GET /api/weekly-report/latest?userId={userId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "用药周报获取成功",
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
    "dailySummaries": [...],
    "aiSummary": "...",
    "missedDrugs": ["降压药"],
    "bestTimeSlot": "早上",
    "needsImprovementTimeSlot": "晚上",
    "fullReportText": "..."
  }
}
```

---

## 组件Props

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Number/String | 是 | 用户ID |

---

## 状态管理

### 内部状态

```javascript
const [report, setReport] = useState(null);          // 周报数据
const [isLoading, setIsLoading] = useState(true);     // 加载状态
const [error, setError] = useState(null);             // 错误信息
const [historyReports, setHistoryReports] = useState([]); // 历史周报
const [showHistory, setShowHistory] = useState(false);    // 显示历史面板
```

---

## 核心函数

### 1. loadLatestReport()
加载最新周报数据

```javascript
const loadLatestReport = async () => {
  setIsLoading(true);
  setError(null);
  
  try {
    const response = await fetch(`/api/weekly-report/latest?userId=${userId}`);
    const data = await response.json();
    
    if (data.code === 200 && data.data) {
      setReport(data.data);
    } else {
      setError(data.message || '获取周报失败');
    }
  } catch (err) {
    setError('网络连接失败，请稍后重试');
  } finally {
    setIsLoading(false);
  }
};
```

### 2. handleScreenshot()
截图并下载PNG图片

```javascript
const handleScreenshot = async () => {
  if (!reportRef.current) return;

  try {
    const canvas = await html2canvas(reportRef.current, {
      backgroundColor: '#ffffff',
      scale: 2,
      useCORS: true,
      logging: false
    });

    const link = document.createElement('a');
    const dateStr = report?.startDate ? `${report.startDate}_${report.endDate}` : 'weekly';
    link.download = `用药周报_${dateStr}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  } catch (err) {
    alert('截图失败，请稍后重试');
  }
};
```

### 3. handleCopyText()
复制完整报告文本到剪贴板

```javascript
const handleCopyText = async () => {
  if (!report?.fullReportText) return;

  try {
    await navigator.clipboard.writeText(report.fullReportText);
    alert('报告已复制到剪贴板！');
  } catch (err) {
    alert('复制失败，请手动选择文本复制');
  }
};
```

---

## 样式设计亮点

### 1. 合规率颜色编码

```javascript
const getComplianceColor = (rate) => {
  if (rate >= 90) return '#16a34a'; // 绿色 - 优秀
  if (rate >= 70) return '#eab308'; // 黄色 - 良好
  if (rate >= 50) return '#f97316'; // 橙色 - 一般
  return '#dc2626';                 // 红色 - 需改进
};
```

### 2. 进度条可视化

```jsx
<div className="progress-bar"
     style={{ 
       width: `${day.complianceRate}%`,
       backgroundColor: dayColor
     }}
></div>
```

### 3. 卡片式布局

每个统计项、时段分析、每日详情都使用独立的卡片，视觉层次清晰。

---

## 响应式设计

### 桌面端（>768px）
- 多列网格布局
- 横向排列的操作按钮
- 完整的间距和留白

### 移动端（≤768px）
- 单列布局
- 垂直排列的按钮
- 缩小的字体和间距
- 优化的触摸区域

---

## 扩展建议

### 1. 添加图表可视化

可以使用Chart.js或Recharts添加：
- 合规率趋势折线图
- 时段对比柱状图
- 药品种类饼图

```bash
npm install chart.js react-chartjs-2
```

### 2. 历史记录功能

后端添加接口：
```java
@GetMapping("/history")
public ResponseResult<List<MedicationWeeklyReport>> getHistory(
    @RequestParam Long userId,
    @RequestParam(required = false) Integer limit) {
    // 返回最近N条周报
}
```

### 3. 分享功能

- 分享到微信（生成小程序码）
- 分享到家属端
- 发送邮件给医生

### 4. 定时生成提醒

```javascript
// 每周一上午9点自动检查是否生成新周报
useEffect(() => {
  const checkAndGenerate = () => {
    const today = new Date();
    if (today.getDay() === 1 && today.getHours() === 9) {
      loadLatestReport();
    }
  };
  
  const interval = setInterval(checkAndGenerate, 3600000); // 每小时检查
  return () => clearInterval(interval);
}, []);
```

---

## 注意事项

### 1. html2canvas依赖

确保已安装：
```bash
npm install html2canvas
```

### 2. CORS问题

如果截图包含跨域图片，需要设置：
```javascript
html2canvas(element, {
  useCORS: true,
  allowTaint: false
});
```

### 3. 性能优化

- 大数据量时使用虚拟滚动
- 图片懒加载
- 防抖处理频繁操作

### 4. 兼容性

- html2canvas不支持CSS Grid（已改用Flexbox）
- Clipboard API需要HTTPS环境
- 旧浏览器可能需要polyfill

---

## 测试建议

### 单元测试

```javascript
import { render, screen, fireEvent } from '@testing-library/react';
import WeeklyReport from './WeeklyReport';

test('加载状态显示', () => {
  render(<WeeklyReport userId={123} />);
  expect(screen.getByText('正在生成AI用药周报...')).toBeInTheDocument();
});

test('错误状态显示', async () => {
  // Mock API失败
  render(<WeeklyReport userId={123} />);
  expect(await screen.findByText(/获取周报失败/)).toBeInTheDocument();
});
```

### 手动测试清单

- [ ] 正常数据显示
- [ ] 空数据处理
- [ ] 错误处理
- [ ] 截图下载功能
- [ ] 复制文本功能
- [ ] 刷新功能
- [ ] 移动端适配
- [ ] 打印效果

---

## 下一步工作

1. ✅ 创建WeeklyReport组件
2. ✅ 创建样式文件
3. ⏳ 集成到App.js主应用
4. ⏳ 添加底部导航标签
5. ⏳ 测试所有功能
6. ⏳ 优化性能和用户体验
7. ⏳ 添加图表可视化（可选）
8. ⏳ 实现历史记录功能（可选）
