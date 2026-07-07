# JWT 鉴权实现全面检查报告

**检查时间：** 2026-07-07
**检查范围：** 前后端 JWT 鉴权机制全链路
**检查方式：** 仅代码审查，未修改任何代码

---

## 一、总体评价

JWT 鉴权的基础框架已搭建完成（JwtUtils 令牌生成/验证、JwtAuthenticationFilter 过滤器、SecurityConfig 安全配置、前端 elderApi/guardianApi 请求封装），但存在**多个严重问题**导致部分功能无法正常使用。核心问题集中在以下三个方面：

1. **PlanServiceImpl 中 getCurrentUserId() 类型转换错误** —— 导致用药计划相关功能全部失效
2. **多个前端 API 调用未携带 JWT 令牌** —— 导致这些接口返回 401/403
3. **用户 ID 概念混淆** —— 数据库主键 `id` 与雪花算法 `userId` 混用，存在安全隐患

---

## 二、严重问题（P0 - 导致功能完全不可用）

### 问题 1：PlanServiceImpl.getCurrentUserId() 类型转换错误 🔴

**文件：** `backend/.../service/impl/PlanServiceImpl.java` 第 36-42 行

**问题代码：**
```java
private Long getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof SysUser) {           // ← 永远为 false
        return ((SysUser) principal).getId();
    }
    throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户未登录");
}
```

**原因分析：**
在 `JwtAuthenticationFilter`（第 44-45 行）中，principal 被设置为 `Long` 类型的 userId：
```java
UsernamePasswordAuthenticationToken authentication =
    new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
```
而 `PlanServiceImpl.getCurrentUserId()` 尝试将 principal 强转为 `SysUser` 对象，类型不匹配，`instanceof SysUser` **永远返回 false**，因此**必定抛出"用户未登录"异常**。

**受影响的功能（全部不可用）：**
- `getTodayPlan()` —— 获取今日用药计划
- `confirmMedication(planId)` —— 确认服药
- `skipMedication(planId)` —— 跳过服药
- `getPendingReminders()` —— 获取待提醒记录

**对比正确实现：** `MedicineController` 和 `GuardianController` 中的 `getCurrentUserId()` 使用 `(Long) authentication.getPrincipal()`，是正确的。

---

### 问题 2：多个前端 API 调用未携带 Authorization 令牌 🔴

以下前端接口使用原生 `fetch` 而非 `authFetch`/`elderFetch`/`guardianFetch`，**未携带 JWT 令牌**，后端 SecurityConfig 要求认证，将返回 401/403：

| 文件 | 行号 | API 路径 | 问题描述 |
|------|------|----------|----------|
| `App.js` | 314 | `/api/emergency/v1/contacts?elderId=...` | 原生 fetch，无 Authorization |
| `App.js` | 494 | `/api/v1/plan/weekly?userId=...` | 原生 fetch，无 Authorization |
| `App.js` | 3032 | `/api/v1/box/list?userId=...` | 原生 fetch，无 Authorization |
| `App.js` | 5845 | `/api/weekly-report/latest?userId=...` | 原生 fetch，无 Authorization |
| `EmergencyAssistant.js` | 169 | `/api/emergency/trigger` | 原生 fetch，无 Authorization |
| `EmergencyAssistant.js` | 217 | `/api/emergency/ask` | 原生 fetch，无 Authorization |
| `GuardianProfile.js` | 27 | `/api/v1/user/profile?userId=...` (GET) | 原生 fetch，无 Authorization |
| `GuardianProfile.js` | 44 | `/api/v1/user/profile?userId=...` (PUT) | 原生 fetch，无 Authorization |
| `GuardianProfile.js` | 78 | `/api/v1/user/password?userId=...` (PUT) | 原生 fetch，无 Authorization |
| `ProfileEdit.js` | 132 | `/api/v1/user/profile?userId=...` (PUT) | 原生 fetch，无 Authorization |
| `App.js` | 2408 | `/api/v1/drug/recognize/batch-upload` | 原生 fetch，仅传 X-User-Id，无 Authorization |
| `App.js` | 2614 | `/api/v1/drug/recognize/upload` | 原生 fetch，仅传 X-User-Id，无 Authorization |
| `App.js` | 3659 | `/api/v1/drug/recognize/history` | 原生 fetch，仅传 X-User-Id，无 Authorization |

**受影响功能：**
- 紧急联系人列表加载
- 一周用药记录查询
- 药箱列表查询（批量添加后刷新）
- 用药周报查询
- 紧急模式触发 + AI 紧急咨询对话
- 家属端个人资料查看/修改/改密码
- 老人端个人资料修改
- 药品图片识别上传 + 识别历史

---

### 问题 3：EmergencyAssistant 未传递 userId 🔴

**文件：** `frontend/src/components/EmergencyAssistant.js` 第 217-233 行

**问题：**
`/api/emergency/ask` 接口的后端实现（`EmergencyController.askEmergencyQuestion`）从请求体中读取 `request.getUserId()`，但前端请求体中**完全没有传递 userId 字段**：

```javascript
body: JSON.stringify({
    question,                    // ✓
    isEmergency: ...,            // ✓
    category: ...,               // ✓
    history: [...],              // ✓
    // ❌ 缺少 userId 字段
}),
```

**后果：** 后端 `userId` 为 null，AI 对话日志无法正确关联用户，可能导致空指针异常或数据丢失。

---

## 三、重要问题（P1 - 安全隐患/数据不一致）

### 问题 4：用户 ID 概念混淆（数据库主键 vs 雪花算法 ID）🟡

系统中存在两种用户标识，使用混乱：

| 标识 | 来源 | 类型 | 说明 |
|------|------|------|------|
| `id` | `BaseEntity.id` | Long | 数据库自增主键，内部使用 |
| `userId` | `SysUser.userId` | Long | 雪花算法生成，对外暴露 |

**JWT 中存储的是 `user.getId()`（数据库主键）**，见 `UserServiceImpl.login()` 第 98 行：
```java
String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
```

**前端各处传递的 ID 不一致：**

| 前端调用 | 传递的值 | 后端期望 | 是否正确 |
|----------|----------|----------|----------|
| `loadEmergencyContacts(user.id)` | 数据库主键 | `selectById` 需数据库主键 | ✓ |
| `loadCalendarPlans(user.userId)` | 雪花 ID | PlanController 接收雪花 ID 后转换 | ✓（但多此一举） |
| `fetchDailyLesson(user.id)` | 数据库主键 | DailyLessonController 的 userId 参数 | ⚠️ 需确认服务层期望 |
| `fetchDailyLesson(regenerate, user.id)` | 数据库主键 | 同上 | ⚠️ 同上 |
| OCR 上传 `X-User-Id: user.userId` | 雪花 ID | OcrController 的 userId 参数 | ⚠️ OcrController 默认存入 recognition log |
| `WeeklyReport userId={user.userId}` | 雪花 ID | WeeklyReportController 的 userId 参数 | ⚠️ 需确认服务层期望 |
| EmergencyController `elderId` | 数据库主键 | `selectById` 需数据库主键 | ✓ |

**安全隐患：** 由于部分控制器（UserController、PlanController、DailyLessonController、WeeklyReportController、EmergencyController、ElderNotificationController、OcrController）仍从前端参数获取 userId，攻击者可以伪造 userId 参数访问**其他用户的数据**，JWT 鉴权形同虚设。

---

### 问题 5：多个后端控制器未从 SecurityContext 获取用户 ID 🟡

**应改为从 SecurityContext 获取用户 ID 的控制器：**

| 控制器 | 当前方式 | 问题 |
|--------|----------|------|
| `UserController` | `@RequestParam String userId` | 可伪造 userId 访问他人资料/改密码 |
| `PlanController` (部分) | `@RequestParam Long userId` | 可伪造 userId 操作他人用药计划 |
| `DailyLessonController` | `@RequestParam Long userId` | 可伪造 userId 获取他人科普记录 |
| `WeeklyReportController` | `@RequestParam Long userId` | 可伪造 userId 获取他人周报 |
| `EmergencyController` | 请求体 `userId` / `@RequestParam` | 可伪造 userId 查询他人对话历史 |
| `ElderNotificationController` | `@RequestParam Long elderId` | 可伪造 elderId 查询他人通知 |
| `OcrController` | `@RequestHeader X-User-Id` | 可伪造 X-User-Id，且 null 时默认 1L |
| `DrugConflictController` | `@RequestParam String userId` | 可伪造 userId 获取他人健康档案 |

**已正确实现的控制器（参考）：**
- `MedicineController` —— 使用 `getCurrentUserId()` 从 SecurityContext 获取
- `GuardianController` —— 使用 `getCurrentUserId()` 从 SecurityContext 获取

---

### 问题 6：SecurityConfig 的 anyRequest().permitAll() 安全风险 🟡

**文件：** `backend/.../config/SecurityConfig.java` 第 69 行

```java
.anyRequest().permitAll()
```

**问题：** 所有未明确配置的路径默认放行，包括：
- `/api/v1/medicine/**` 的 PATCH 方法（只配置了 GET/POST/PUT/DELETE）
- 未来新增的接口默认无需认证
- `/api/v1/user/profile` 的其他 HTTP 方法

**建议：** 应改为 `.anyRequest().authenticated()`，采用"白名单"策略。

---

### 问题 7：OcrController userId 为 null 时默认使用 1L 🟡

**文件：** `backend/.../controller/OcrController.java` 第 59-61 行、98-100 行

```java
if (userId == null) {
    userId = 1L;  // ← 默认使用 ID 为 1 的用户
}
```

**问题：** 当 X-User-Id 头缺失时，识别记录会错误地归到 ID=1 的用户名下，造成数据污染。

---

### 问题 8：Spring Security 缺少 JSON 格式的 401/403 响应处理 🟡

**文件：** `backend/.../config/SecurityConfig.java`

**问题：** 未配置自定义的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`。当请求未通过认证（401）或授权（403）时，Spring Security 默认返回 HTML 格式的错误页面，而前端期望接收 JSON 格式的 `ResponseResult`。

**后果：** 前端 `response.json()` 解析可能失败，导致错误处理逻辑无法正确触发。

---

## 四、次要问题（P2 - 代码质量/一致性）

### 问题 9：JWT 密钥硬编码在 application.properties 中 🟢

**文件：** `application.properties` 第 94 行
```properties
jwt.secret=elderly-medication-jwt-secret-key-2024-very-long-secret-key-for-security
```
密钥已提交到 Git，应迁移到 `application-local.properties` 或通过环境变量注入。

### 问题 10：前端 401 检测逻辑不统一 🟢

`elderApi.js` 检测条件包含 `data.message?.includes('Access Denied')`，而 `guardianApi.js` 不包含。`App.js` 中的 `authFetch` 包含该条件但缺少 `data.message?.includes('未认证')`。建议统一。

### 问题 11：OPTIONS 预检请求未显式放行 🟢

`SecurityConfig` 未显式配置 `.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()`，虽然 CorsFilter Bean 会处理 CORS，但建议显式配置以避免潜在问题。

### 问题 12：@CrossOrigin(origins = "*") 与 CorsFilter 重复 🟢

多个控制器（AiController、EmergencyController、DailyLessonController、DrugConflictController、WeeklyReportController、ElderNotificationController）标注了 `@CrossOrigin(origins = "*")`，与全局 CorsConfig 重复，且 `origins = "*"` 与 `allowCredentials = true` 冲突。

---

## 五、受影响功能清单

### 完全不可用（P0）：
1. ❌ 今日用药计划加载/确认服药/跳过服药/待提醒记录 —— PlanServiceImpl 类型转换错误
2. ❌ AI 紧急咨询对话 —— 未传 JWT + 未传 userId
3. ❌ 紧急模式触发 —— 未传 JWT
4. ❌ 紧急联系人列表加载 —— 未传 JWT
5. ❌ 一周用药记录查询 —— 未传 JWT
6. ❌ 用药周报查询 —— 未传 JWT
7. ❌ 家属端个人资料查看/修改/改密码 —— 未传 JWT
8. ❌ 老人端个人资料修改 —— 未传 JWT
9. ❌ 药品图片识别上传 —— 未传 JWT
10. ❌ 识药历史查询 —— 未传 JWT

### 可用但存在安全隐患（P1）：
1. ⚠️ 用户资料查看/修改 —— 可伪造 userId
2. ⚠️ 用药计划生成 —— 可伪造 userId
3. ⚠️ 今日一课 —— 可伪造 userId
4. ⚠️ 药品冲突检测（结合档案）—— 可伪造 userId
5. ⚠️ 老人端通知 —— 可伪造 elderId

### 正常工作：
1. ✅ 登录/注册
2. ✅ 药箱管理（MedicineController）—— 正确使用 SecurityContext
3. ✅ 家属端仪表盘/老人详情/通知 —— 正确使用 guardianApi
4. ✅ 药品字典查询（DrugController）—— 公开接口，无需认证

---

## 六、修复建议优先级

### 第一优先级（修复后核心功能恢复）：
1. **修复 PlanServiceImpl.getCurrentUserId()** —— 改为 `(Long) principal`，与 MedicineController 一致
2. **前端所有原生 fetch 改为 authFetch/elderFetch/guardianFetch** —— 统一携带 JWT 令牌
3. **EmergencyAssistant 请求体添加 userId** —— 或改为后端从 SecurityContext 获取

### 第二优先级（安全加固）：
4. **所有后端控制器改为从 SecurityContext 获取用户 ID** —— 移除前端 userId 参数
5. **SecurityConfig 改为 anyRequest().authenticated()** —— 白名单策略
6. **添加 JSON 格式的 401/403 响应处理** —— 自定义 AuthenticationEntryPoint 和 AccessDeniedHandler
7. **移除 OcrController 的 userId=1L 默认值**

### 第三优先级（代码质量）：
8. 统一用户 ID 概念，明确数据库主键与雪花 ID 的使用场景
9. JWT 密钥迁移到环境变量
10. 统一前端 401 检测逻辑
11. 清理重复的 @CrossOrigin 注解

---

*本报告仅用于检查分析，未修改任何代码。请确认后告知是否需要我进行修复。*
