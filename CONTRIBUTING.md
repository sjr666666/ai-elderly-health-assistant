# 贡献指南

首先感谢你愿意为这个项目贡献代码!无论是修 bug、加功能、改进文档还是提建议,都非常欢迎。

## 📌 快速导航

- [提问 / 报告 Bug](#报告-bug)
- [提出新功能](#提出新功能)
- [提交代码 (Pull Request)](#提交代码-pull-request)
- [开发环境搭建](#开发环境搭建)
- [代码规范](#代码规范)

---

## 报告 Bug

在提交 Issue 之前,请先:

1. **搜索是否已有相同 Issue**(包括已关闭的),避免重复提交;
2. 确认你使用的是**最新版本**代码(`git pull` 后复现)。

提交 Bug 时请包含以下信息(可直接使用 [Bug 模板](./.github/ISSUE_TEMPLATE/bug_report.yml)):

- 环境信息:操作系统、Node.js / JDK / MySQL 版本;
- 复现步骤(越详细越好);
- 预期行为与实际行为;
- 错误日志 / 截图;
- 如果是前端问题,请说明浏览器及版本。

## 提出新功能

欢迎提交 Feature Request!请说明:

- 这个功能要解决什么问题(痛点场景);
- 建议的实现思路(可选);
- 是否与现有功能冲突。

维护者会评估后回复,标注 `good first issue` 的 Issue 尤其适合新手认领。

## 提交代码 (Pull Request)

### 推荐流程

1. **Fork** 本仓库到你的账号;
2. 克隆你的 fork 并添加上游仓库:

   ```bash
   git clone https://github.com/<你的用户名>/aaagame.git
   cd aaagame
   git remote add upstream https://github.com/sjr666666/aaagame.git
   ```

3. 基于最新 `main` 创建功能分支(**命名规范**:`feat/xxx`、`fix/xxx`、`docs/xxx`、`refactor/xxx`):

   ```bash
   git fetch upstream
   git checkout -b feat/your-feature upstream/main
   ```

4. 编写代码并**自测通过**;
5. 提交前检查代码规范(见下文),并运行前端 `npm run lint` 与后端 `mvn checkstyle:check`;
6. 提交(遵循[约定式提交](https://www.conventionalcommits.org/zh-hans/)规范):

   ```bash
   git commit -m "feat(medication): 添加xxx功能"
   ```

7. 推送到你的 fork 并创建 Pull Request,PR 描述请参考 [PR 模板](./.github/PULL_REQUEST_TEMPLATE.md);
8. 等待 review。如果维护者提出修改意见,在同一个分支上继续提交即可,无需关闭 PR。

### PR 注意事项

- 一个 PR 只做一件事,保持改动聚焦,方便 review 与回滚;
- 大改动请先开 Issue 讨论方案,避免返工;
- 不要随意格式化与本次改动无关的代码(避免 diff 噪音);
- 涉及数据库变更的,请遵循 `.trae/rules/database-change.md` 规范并补充迁移说明;
- 新功能请同步更新 README(如 API 列表、数据库表)。

## 开发环境搭建

见 [README 快速开始](./README.md#快速开始):

- Node.js >= 16.x、JDK >= 11、MySQL >= 8.0、Maven >= 3.6;
- 数据库初始化脚本(`backend/init_database.bat` 或 `backend/init_database.sh`);
- 本地配置 `application-local.properties`(从 example 复制,填入 MySQL 密码与可选第三方 Key)。

> 第三方服务(百度 OCR / TTS、DeepSeek、阿里云 OSS)均为可选,留空不影响核心功能。

## 代码规范

- **提交前必须通过**:前端 `npm run lint`、后端 `mvn checkstyle:check`(CI 也会执行,不过会失败);
- **提交信息**:遵循[约定式提交](https://www.conventionalcommits.org/zh-hans/),格式 `type(scope): subject`;
- **枚举规范**:禁止硬编码状态字符串,统一通过枚举引用(参考 `com.example.backend.model.enums` 包);
- **安全**:不要把密钥、token 提交到仓库,敏感配置一律放 `application-local.properties`(已被 `.gitignore` 忽略);
- **数据库变更**:遵循 `.trae/rules/database-change.md` 规范,SQL 脚本需可重复执行。

## 行为准则

参与本项目即表示你同意我们的[行为准则](./CODE_OF_CONDUCT.md):保持友善、尊重他人、就事论事。对违反者,维护者有权关闭相关 Issue / PR。

---

**再次感谢你的贡献!** ⭐ 如果你觉得项目有用,也欢迎给仓库点个 Star,帮助更多人看到它。
