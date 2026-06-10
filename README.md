# AI紧急助手系统

## 项目简介

AI紧急助手系统是一款专为老年人设计的智能健康助手，提供紧急情况下的急救指导和日常健康咨询服务。

## 技术栈

### 前端
- React 18
- React Scripts 5.0.1
- Element Plus
- http-proxy-middleware 2.0.9

### 后端
- Spring Boot 2.7.18
- MyBatis Plus 3.5.5
- MySQL 8.0 + Redis 6.0
- Neo4j（药品冲突图谱）
- Spring Security（密码加密）
- Hutool 工具库
- 阿里云 OSS（药品图片存储）
- 百度 OCR（药品包装识别）
- 百度 TTS（语音播报）
- DeepSeek AI（紧急咨询 / 用药指导 / 冲突分析）

## 快速开始

### 环境要求

- Node.js >= 16.x
- JDK >= 11
- MySQL >= 8.0
- Redis >= 6.0

### 1. 克隆项目

```bash
git clone <仓库地址>
cd aaagame
```

### 2. 后端配置

#### 数据库初始化（推荐）

**Windows 用户**：

```bash
# 在项目根目录或 backend 目录运行均可
cd backend
init_database.bat

# 或跳过完整药品数据（仅导入基础数据，加快速度）
init_database.bat --skip-drug-data

# 或指定数据库连接信息
init_database.bat -u root -p your_password
```

**macOS / Linux 用户**：

```bash
# 在项目根目录或 backend 目录运行均可
cd backend
chmod +x init_database.sh
./init_database.sh

# 或指定参数
./init_database.sh -u root -p your_password
```

脚本会依次执行：
1. `backend/src/main/resources/init_database.sql`（v2.1）—— 创建数据库表结构 + 基础测试数据（5 个用户、14 个常用药品、用药计划、冲突规则等）。`sys_user` / `drug_category_keywords` / `drug_aliases` 已改用 `INSERT IGNORE`，可安全重复执行。
2. `backend/src/main/resources/init_drug_data.sql`（v3.0）—— 补充完整药品数据（约 90+ 种常见药品），使用 `INSERT IGNORE` 可重复执行。

> 两个 SQL 脚本**可重复执行**：`init_database.sql` 对业务表用 `DROP+CREATE` 重建，对保留数据的字典表用 `INSERT IGNORE` 跳过已存在数据；`init_drug_data.sql` 通过 `INSERT IGNORE` 跳过已存在数据。

**手动初始化**（不推荐，仅在脚本不可用时）：

```bash
cd backend
mysql -u root -p elderly_medication < src/main/resources/init_database.sql
mysql -u root -p elderly_medication < src/main/resources/init_drug_data.sql
```

#### 本地配置文件

首次运行前，需要创建本地配置文件 `application-local.properties` 配置数据库密码和**第三方 API 密钥**。该文件已被 `.gitignore` 忽略，不会提交到仓库。

**推荐方式**：从模板复制，然后填写真实值。

```bash
# 1. 复制模板
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties

# 2. 编辑 application-local.properties，填入下方"敏感凭据"中列出的所有值
```

#### 敏感凭据

下面这些密钥**不要提交到任何代码、聊天、issue、工单里**。每位团队成员各自在云厂商控制台创建/获取本地副本：

| 服务 | 字段 | 申请地址 | 权限建议 |
|---|---|---|---|
| MySQL | `spring.datasource.password` | 本地 | — |
| 阿里云 OSS | `aliyun.oss.access-key-id`<br>`aliyun.oss.access-key-secret` | https://ram.console.aliyun.com | 仅授权 `cxcy-aa` bucket |
| 百度 OCR | `baidu.ocr.app-id`<br>`baidu.ocr.api-key`<br>`baidu.ocr.secret-key` | https://console.bce.baidu.com → 文字识别 OCR | 按需勾选能力 |
| 百度 TTS | `baidu.tts.appId`<br>`baidu.tts.apiKey`<br>`baidu.tts.secretKey` | https://console.bce.baidu.com → 短文本在线合成 | — |
| DeepSeek | `deepseek.api-key` | https://platform.deepseek.com | — |

填好的 `application-local.properties` 大致长这样（**真实密钥不要粘贴到任何公共地方**）：

```properties
spring.datasource.password=你的MySQL密码

aliyun.oss.access-key-id=LTAI5t...
aliyun.oss.access-key-secret=...

baidu.ocr.app-id=123456789
baidu.ocr.api-key=...
baidu.ocr.secret-key=...

baidu.tts.appId=123456789
baidu.tts.apiKey=...
baidu.tts.secretKey=...

deepseek.api-key=sk-...
```

> 模板文件 `application-local.properties.example` 包含完整字段占位，可作为参考。

#### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动后访问: http://localhost:8080

#### 测试账号

数据库初始化后可使用以下测试账号登录（密码统一为 `123456`）：

| 用户名 | 角色 | 备注 |
|---|---|---|
| `laowang` | 老人 | 王阿姨，有家庭药箱与用药计划 |
| `laoli` | 老人 | 李大爷，冠心病/脑梗后遗症用药 |
| `zhangsan` | 家属 | 张三，绑定 laowang |
| `zhaosi` | 家属 | 赵四，绑定 laoli |
| `xiaomei` | 家属 | 小美，磺胺过敏史 |

### 3. 前端配置

```bash
cd frontend
npm install
```

**重要提示**：如果遇到代理配置问题，请执行以下步骤：

```bash
# 删除旧依赖和锁定文件
rm -rf node_modules
rm package-lock.json

# 重新安装依赖
npm install

# 启动开发服务器
npm start
```

前端服务启动后访问: http://localhost:3000

## 团队成员操作步骤

### 首次拉取代码

```bash
# 1. 克隆仓库
git clone <仓库地址>
cd aaagame

# 2. 安装后端依赖（如果需要）
cd backend
mvn clean install -DskipTests

# 3. 安装前端依赖
cd ../frontend
npm install

# 4. 启动后端（终端1）
cd ../backend
mvn spring-boot:run

# 5. 启动前端（终端2）
cd ../frontend
npm start
```

### 遇到代理问题的解决方案

如果登录或API请求失败，出现404错误，可能是代理配置问题：

```bash
# 1. 删除旧的依赖和锁定文件
rm -rf node_modules
rm package-lock.json

# 2. 重新安装依赖
npm install

# 3. 启动开发服务器
npm start
```

**原因说明**：`http-proxy-middleware` 版本与 `react-scripts` 存在兼容性问题，已在 `package.json` 中锁定版本为 `2.0.9`。删除 `package-lock.json` 后重新安装可确保使用正确版本。

## 功能特性

### 🤖 AI 紧急咨询
- ✅ 紧急情况智能问答（自动判断问题是否紧急）
- ✅ 多分类标签引导（用药 / 症状 / 急救等）
- ✅ 对话历史记录与上下文记忆
- ✅ AI 老年友好用药指导生成（DeepSeek）

### 💊 药品管理
- ✅ 药品字典查询（关键词 / 类别 / 别名）
- ✅ AI 智能搜索（支持别名、商品名解析）
- ✅ 家庭药箱 CRUD（增删改查、状态过滤）
- ✅ 药品图片识别（百度 OCR + 阿里云 OSS）
- ✅ 批量药品图片识别与确认入库
- ✅ 药品到期 / 过期自动提醒（30 天预警）

### ⚠️ 药品冲突检测
- ✅ 多药品冲突检测（DeepSeek AI 深度分析）
- ✅ 本地规则快速检测（不依赖 AI，适合自动场景）
- ✅ 结合健康档案个性化分析（BMI / 慢病 / 肝肾功能 / 烟酒史等）
- ✅ 全面冲突分析（药品 + 保健品 + 食物 + 饮料）
- ✅ 新药入箱自动检测并告警

### 📅 用药计划与提醒
- ✅ 今日 / 本周用药计划视图
- ✅ 计划执行（确认 / 跳过 / 撤销，幂等操作）
- ✅ 根据家庭药箱自动生成每日计划
- ✅ 定时任务 + 前端轮询双通道提醒
- ✅ 用药记录追溯

### 👤 用户与健康档案
- ✅ 账号注册 / 登录（密码加密、雪花算法 ID）
- ✅ 健康档案管理（年龄 / 身高体重 / 慢病 / 过敏史 / 肝肾功能 / 孕期哺乳 / 烟酒史）
- ✅ 家属角色与老人账号绑定

### 📞 紧急联系人
- ✅ 多联系人增删改查
- ✅ 首个联系人自动设为主要联系人
- ✅ 一键呼叫家人（预留功能）

### 🔊 多模态与体验
- ✅ 百度 TTS 语音播报（语速可调，老年人友好）
- ✅ 老年友好界面（大字体、高对比度）
- ✅ 离线模式支持（Service Worker）
- ✅ 前后端分离 + 跨域配置

## 项目结构

```
aaagame/
├── backend/                 # 后端服务
│   ├── src/main/java/       # Java源代码
│   ├── src/main/resources/  # 配置文件
│   └── pom.xml              # Maven配置
├── frontend/                # 前端应用
│   ├── src/                 # React源代码
│   ├── public/              # 静态资源
│   └── package.json         # 前端依赖
└── README.md                # 项目说明
```

## 注意事项

1. **端口占用**：确保端口 8080（后端）和 3000（前端）未被占用
2. **数据库连接**：首次启动前需配置正确的数据库连接信息
3. **代理配置**：前端使用代理转发API请求，确保 `setupProxy.js` 配置正确
4. **依赖版本**：请勿修改 `http-proxy-middleware` 的版本号，否则可能导致代理失效
5. **数据库更新**：从旧版本更新时，需要执行以下SQL清空旧数据：
   ```sql
   DELETE FROM medication_plan;
   ```
   或者调用API：`DELETE http://localhost:8080/api/v1/plan/clear-all`
   **原因**：旧版本中的用药计划数据格式与新版本不兼容，清空后可重新添加正确的用药计划

## 开发规范

- 代码提交前请运行 `npm run lint`（前端）和 `mvn checkstyle:check`（后端）
- 遵循项目的代码风格和命名规范
- 提交信息请遵循约定式提交规范

## 联系方式

如有问题，请联系项目负责人。
