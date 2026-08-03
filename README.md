# AI紧急助手系统

## 项目简介

AI紧急助手系统是一款专为老年人设计的智能健康助手，提供紧急情况下的急救指导、日常健康咨询、用药管理和家属监护等功能。系统分为**老人端**和**家属端**双端设计，老人端侧重用药管理与紧急求助，家属端侧重远程监护与通知接收。

## 技术栈

### 前端
- React 18 + React Scripts 5.0.1
- http-proxy-middleware 2.0.9
- 家属端：移动端风格（max-width: 480px，底部Tab导航）

### 后端
- Spring Boot 2.7.18 + MyBatis Plus 3.5.5
- MySQL 8.0
- Spring Security（BCrypt 密码加密）
- Hutool 工具库
- 阿里云 OSS（药品图片存储，可选）
- 百度 OCR（药品包装识别）
- 百度 TTS（语音播报）
- DeepSeek AI（紧急咨询 / 用药指导 / 冲突分析）

## 快速开始

### 环境要求

- Node.js >= 16.x
- JDK >= 11
- MySQL >= 8.0
- Maven >= 3.6

### 1. 克隆项目

```bash
git clone <仓库地址>
cd innovative-ideas-challenge
```

### 2. 数据库初始化

**Windows 用户**：

```bash
cd backend
init_database.bat

# 或跳过完整药品数据（仅导入基础数据，加快速度）
init_database.bat --skip-drug-data

# 或指定数据库连接信息
init_database.bat -u root -p your_password
```

**macOS / Linux 用户**：

```bash
cd backend
chmod +x init_database.sh
./init_database.sh

# 或指定参数
./init_database.sh -u root -p your_password
```

脚本会依次执行以下SQL（**必须按顺序执行**）：

| 顺序 | 脚本文件 | 说明 |
|------|----------|------|
| 1 | `init_database.sql` | 创建数据库 + 16张基础表 + 测试数据 |
| 2 | `init_drug_data.sql` | 补充完整药品数据（约76种常见药品） |
| 3 | `init_guardian_tables.sql` | 家属端3张表（关联/通知/紧急事件）+ 外键 |

> 所有SQL脚本**可重复执行**：建表用 `CREATE TABLE IF NOT EXISTS`，数据用 `INSERT IGNORE`，外键用存储过程安全添加。

**手动初始化**（不推荐，仅在脚本不可用时）：

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/init_database.sql
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/init_drug_data.sql
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/init_guardian_tables.sql
```

### 3. 后端配置

创建本地配置文件 `application-local.properties`，该文件已被 `.gitignore` 忽略：

```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```

编辑 `application-local.properties`，填入以下配置：

```properties
# MySQL密码（必填）
spring.datasource.password=你的MySQL密码

# 阿里云OSS（可选，留空则禁用）
aliyun.oss.access-key-id=
aliyun.oss.access-key-secret=

# 百度OCR（药品包装识别）
baidu.ocr.app-id=your_baidu_ocr_app_id
baidu.ocr.api-key=your_baidu_ocr_api_key
baidu.ocr.secret-key=your_baidu_ocr_secret_key

# 百度TTS（语音播报）
baidu.tts.appId=your_baidu_tts_app_id
baidu.tts.apiKey=your_baidu_tts_api_key
baidu.tts.secretKey=your_baidu_tts_secret_key

# DeepSeek大模型（紧急咨询/用药指导/冲突分析）
deepseek.api-key=sk-your_deepseek_api_key_here
```

> 阿里云OSS的 `access-key-id` 留空时，系统会自动禁用OSS功能，不影响其他功能使用。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动后访问: http://localhost:8080

### 5. 启动前端

```bash
cd frontend
npm install
npm start
```

前端服务启动后访问: http://localhost:3000

> 如果遇到代理问题，删除 `node_modules` 和 `package-lock.json` 后重新 `npm install`。

### 6. 测试账号

数据库初始化后可使用以下测试账号登录（密码统一为 `123456`）：

| 用户名 | 角色 | 姓名 | 备注 |
|--------|------|------|------|
| `laowang` | 老人 | 王阿姨 | 有家庭药箱与用药计划 |
| `laoli` | 老人 | 李大爷 | 冠心病/脑梗后遗症用药 |
| `zhangsan` | 家属 | 张三 | 绑定王阿姨 |
| `zhaosi` | 家属 | 赵四 | 绑定李大爷 |
| `xiaomei` | 家属 | 小美 | 磺胺过敏史 |

> 登录后系统根据角色自动跳转：老人 → 老人端首页，家属 → 家属端移动端页面。

## 功能特性

### 老人端

#### AI 紧急咨询
- 紧急情况智能问答（自动判断问题是否紧急）
- 多分类标签引导（用药 / 症状 / 急救等）
- 对话历史记录与上下文记忆
- AI 老年友好用药指导生成（DeepSeek）

#### 药品管理
- 药品字典查询（关键词 / 类别 / 别名）
- AI 智能搜索（支持别名、商品名解析）
- 家庭药箱 CRUD（增删改查、状态过滤）
- 药品图片识别（百度 OCR + 阿里云 OSS）
- 批量药品图片识别与确认入库
- 药品到期 / 过期自动提醒（30 天预警）

#### 药品冲突检测
- 多药品冲突检测（DeepSeek AI 深度分析）
- 本地规则快速检测（不依赖 AI，适合自动场景）
- 结合健康档案个性化分析（BMI / 慢病 / 肝肾功能 / 烟酒史等）
- 新药入箱自动检测并告警

#### 用药计划与提醒
- 今日 / 本周用药计划视图
- 计划执行（确认 / 跳过 / 撤销，幂等操作）
- 根据家庭药箱自动生成每日计划
- 定时任务 + 前端轮询双通道提醒
- 用药记录追溯

#### 用户与健康档案
- 账号注册 / 登录（BCrypt 密码加密）
- 健康档案管理（年龄 / 身高体重 / 慢病 / 过敏史 / 肝肾功能 / 孕期哺乳 / 烟酒史）

#### 紧急联系人
- 多联系人增删改查
- 首个联系人自动设为主要联系人

#### 多模态与体验
- 百度 TTS 语音播报（语速可调，老年人友好）
- 老年友好界面（大字体、高对比度）
- 离线模式支持（Service Worker）

### 家属端（移动端风格）

#### 仪表盘
- 关联老人数量、紧急事件数、通知数统计
- 今日用药状态实时显示（已服/待服/漏服）
- 老人列表快捷入口

#### 老人详情
- 老人基本信息与健康档案查看
- 紧急事件列表与处理（标记已处理）
- 临期药品预警
- 绑定/解绑老人

#### 通知记录
- 短信通知记录查看（漏服提醒、紧急事件、临期药品）
- 通知状态与发送详情

#### 绑定管理
- 通过老人用户名绑定/解绑
- 解绑后重新绑定自动恢复关联

## 项目结构

```
innovative-ideas-challenge/
├── backend/                          # 后端服务（Spring Boot）
│   ├── src/main/java/com/example/backend/
│   │   ├── common/                   # 通用工具（ResponseResult/SnowflakeId等）
│   │   ├── config/                   # 配置类（CORS/Security/OSS/OCR/TTS等）
│   │   ├── controller/               # 控制器层
│   │   │   ├── GuardianController    # 家属端API（9个端点）
│   │   │   ├── DrugController        # 药品管理
│   │   │   ├── PlanController        # 用药计划
│   │   │   ├── EmergencyController   # 紧急求助
│   │   │   ├── AiController          # AI对话
│   │   │   └── ...                   # 其他控制器
│   │   ├── mapper/                   # MyBatis Plus Mapper
│   │   ├── model/
│   │   │   ├── dto/                  # 数据传输对象
│   │   │   ├── entity/               # 数据库实体（15个）
│   │   │   └── vo/                   # 视图对象
│   │   ├── service/                  # 服务接口
│   │   │   └── impl/                 # 服务实现
│   │   └── task/                     # 定时任务
│   ├── src/main/resources/
│   │   ├── init_database.sql         # 建库+基础表+测试数据
│   │   ├── init_drug_data.sql        # 完整药品数据
│   │   ├── init_guardian_tables.sql  # 家属端表
│   │   ├── application.properties    # 主配置
│   │   └── application-local.properties.example  # 本地配置模板
│   ├── init_database.bat             # Windows初始化脚本
│   ├── init_database.sh              # Linux/Mac初始化脚本
│   └── pom.xml
├── frontend/                         # 前端应用（React）
│   ├── src/
│   │   ├── components/
│   │   │   ├── guardian/             # 家属端组件（移动端风格）
│   │   │   │   ├── GuardianApp.js    # 家属端主框架（Tab导航）
│   │   │   │   ├── GuardianLogin.js  # 家属端登录
│   │   │   │   ├── GuardianDashboard.js  # 仪表盘
│   │   │   │   ├── GuardianElderDetail.js # 老人详情
│   │   │   │   ├── GuardianNotification.js # 通知记录
│   │   │   │   └── guardian.css      # 家属端样式
│   │   │   ├── Login.js              # 老人端登录
│   │   │   ├── DrugListView.js       # 药箱管理
│   │   │   ├── EmergencyAssistant.js # AI紧急助手
│   │   │   └── ...                   # 其他老人端组件
│   │   ├── App.js                    # 根组件（角色路由）
│   │   └── setupProxy.js             # API代理配置
│   └── package.json
└── README.md
```

## API 概览

### 家属端 API（GuardianController）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/guardian/dashboard` | GET | 仪表盘数据 |
| `/api/v1/guardian/elders` | GET | 关联老人列表 |
| `/api/v1/guardian/elders/{id}` | GET | 老人详情 |
| `/api/v1/guardian/bind` | POST | 绑定老人 |
| `/api/v1/guardian/unbind` | DELETE | 解绑老人 |
| `/api/v1/guardian/elders/{id}/events` | GET | 紧急事件列表 |
| `/api/v1/guardian/events/{id}/resolve` | PUT | 处理紧急事件 |
| `/api/v1/guardian/notifications` | GET | 通知记录 |
| `/api/v1/guardian/elders/{id}/expiring-drugs` | GET | 临期药品 |

### 老人端主要 API

| 模块 | 端点前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/v1/user` | 注册/登录/档案 |
| 药品 | `/api/v1/drug` | 药品搜索/药箱管理 |
| 计划 | `/api/v1/plan` | 用药计划/服药确认 |
| AI | `/api/v1/ai` | 紧急咨询/用药指导 |
| OCR | `/api/v1/ocr` | 药品图片识别 |
| 冲突 | `/api/v1/drug-conflict` | 药品冲突检测 |
| 紧急 | `/api/v1/emergency` | 紧急联系人/SOS |

## 数据库表结构

| 表名 | 说明 | 所属脚本 |
|------|------|----------|
| `sys_user` | 用户表（老人+家属） | init_database.sql |
| `drug_base` | 药品基础库 | init_database.sql |
| `user_medicine_box` | 家庭药箱 | init_database.sql |
| `medication_plan` | 用药计划 | init_database.sql |
| `medication_log` | 服药确认记录 | init_database.sql |
| `drug_conflict_rules` | 药品冲突规则 | init_database.sql |
| `drug_category_keywords` | 药品类别关键词 | init_database.sql |
| `drug_aliases` | 药品别名映射 | init_database.sql |
| `ocr_record` | OCR识别记录 | init_database.sql |
| `drug_recognition_log` | 药品识别日志 | init_database.sql |
| `ai_conversation_log` | AI对话记录 | init_database.sql |
| `reminder_log` | 提醒通知记录 | init_database.sql |
| `emergency_contact` | 紧急联系人 | init_database.sql |
| `guardian_elder_relation` | 家属-老人关联 | init_guardian_tables.sql |
| `sms_notification_log` | 短信通知日志 | init_guardian_tables.sql |
| `emergency_event` | 紧急事件 | init_guardian_tables.sql |

## 注意事项

1. **端口占用**：确保端口 8080（后端）和 3000（前端）未被占用
2. **数据库连接**：首次启动前需配置正确的数据库连接信息
3. **代理配置**：前端使用 `setupProxy.js` 代理转发API请求，请勿修改 `http-proxy-middleware` 版本号
4. **阿里云OSS**：`access-key-id` 留空时系统自动禁用OSS，不影响其他功能
5. **Neo4j**：已排除自动配置，不影响项目启动
6. **数据库更新**：从旧版本更新时，需按顺序重新执行三个SQL脚本

## 开发规范

- 代码提交前请运行 `npm run lint`（前端）和 `mvn checkstyle:check`（后端）
- 遵循项目的代码风格和命名规范
- 提交信息请遵循约定式提交规范
- 数据库变更请遵循 `.trae/rules/database-change.md` 规范
