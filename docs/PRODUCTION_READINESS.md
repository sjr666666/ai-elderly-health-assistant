# 生产化上线准备说明

本项目是一套基于单台服务器的生产化演练系统，面向老年人用药管理场景。
它不是医疗器械，AI 输出不能替代医生或其他具备资质的专业人员判断。

## 当前已完成的生产化改造

- JWT 密钥启动时强校验，密码使用 BCrypt 哈希存储。
- 统一 CORS 白名单和安全响应头。
- WebSocket 握手阶段校验 JWT，不信任前端传入的 `elderId`。
- OCR 任务和批量识别结果增加用户归属校验。
- 上传图片校验大小、扩展名、Content-Type 和文件头。
- 对外返回统一错误信息，详细异常只写入服务端日志。
- 通过 `X-Request-Id` 实现请求链路关联。
- AI 接口使用 Redis 限流，Redis 不可用时降级为本地限流。
- 定时任务使用 Redis 分布式锁，避免多实例重复执行。
- 生产 Docker Compose 使用 MySQL/Redis 内部网络、健康检查、最小权限数据库账号、持久化数据卷和 Nginx。
- GitHub Actions 已配置前端构建、后端测试和密钥扫描。

## 部署配置和密钥要求

将 `.env.example` 复制为只用于部署的 `.env`，并替换所有占位值。

`.env` 禁止提交到 Git。生产环境必须满足：

- `SPRING_PROFILES_ACTIVE=prod`
- 后端使用非 root 数据库账号
- Redis 设置独立密码
- `APP_CORS_ALLOWED_ORIGINS` 使用明确的前端域名
- JWT、手机号加密密钥和第三方 API Key 通过环境变量注入

## 发布检查清单

1. 在 `frontend` 目录执行 `npm.cmd run build`。
2. 在 `backend` 目录执行 `mvn -B test`。
3. 加载生产环境变量后执行 `docker compose config`。
4. 执行 `docker compose up -d --build` 启动服务。
5. 访问 `/actuator/health`，确认返回 `{"status":"UP"}`。
6. 验证登录、角色权限、服药确认、库存扣减、OCR 任务归属和 WebSocket 身份认证。
7. 使用 `scripts/backup-mysql.ps1` 创建数据库备份。
8. 记录当前镜像版本，并保留上一版本用于回滚。

## 本地开发热加载

修改代码时使用开发 Compose 配置：

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

打开 `http://localhost:3000`：

- 修改 `frontend/src` 后，React 会自动刷新页面。
- 修改 `backend/src` 后，容器会自动编译 Java 源码并重启 Spring Boot。
- MySQL 和 Redis 仍然运行在 Docker 中，数据通过持久化卷保存。

生产模式使用独立的 `docker-compose.yml`，通过 Nginx 提供 `http://localhost`。

## 尚未达到完整企业级平台的部分

- 已实现 Refresh Token 轮换、HttpOnly Cookie 保存和退出登录主动撤销；完整的设备管理和密钥轮换仍需补充。
- 已提供 HTTPS Nginx 配置和证书挂载模板；真实证书申请和自动续期交给云负载均衡或证书服务。
- OCR 已接入 Redis Streams 消费组，任务状态仍以数据库为准；AI 后续可复用同一任务队列抽象。
- 通知已采用 Outbox 表、后台发送、Redis 分布式锁和失败重试；短信等渠道仍需接入独立消费者。
- 备份脚本已支持 7z 加密和 ossutil 异地上传；正式环境还需配置密钥托管和定期恢复演练。
- 已引入 Flyway，并用 baseline 兼容现有演示数据库；后续迁移必须使用新的版本脚本，不再修改已执行脚本。

## 面试讲解主线

核心业务流程是：

```text
上传药品图片
  -> OCR 识别
  -> 药品匹配
  -> 写入家庭药箱
  -> 生成用药计划
  -> 用户确认服药
  -> 扣减库存并记录日志
  -> 漏服提醒
  -> 通知家属
```

生产化改造重点解决了：

- 用户数据归属和越权访问
- 第三方服务失败隔离
- 定时任务幂等执行
- 敏感配置与业务代码分离
- Docker 部署和健康检查
- 日志、测试和发布可观测性

当前项目优先采用模块化单体架构，没有为了拆分微服务而增加不必要的复杂度。
