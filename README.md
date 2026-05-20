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
- MyBatis Plus
- MySQL
- Redis
- DeepSeek AI API
- 百度TTS

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

#### 数据库配置
```bash
# 创建数据库
CREATE DATABASE ai_emergency_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 初始化数据（可选）
cd backend
mysql -u root -p ai_emergency_assistant < src/main/resources/data/init_data.sql
```

#### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动后访问: http://localhost:8080

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

- ✅ 紧急情况急救指导
- ✅ 日常健康咨询
- ✅ 老年友好界面设计（大字体、高对比度）
- ✅ 对话历史记录
- ✅ 离线模式支持
- ✅ 一键呼叫家人（预留功能）

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

## 开发规范

- 代码提交前请运行 `npm run lint`（前端）和 `mvn checkstyle:check`（后端）
- 遵循项目的代码风格和命名规范
- 提交信息请遵循约定式提交规范

## 联系方式

如有问题，请联系项目负责人。
