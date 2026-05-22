# 用户登录和档案管理接口说明

## 📋 概述

本文档描述了用户登录和个人档案管理的完整流程，包括前后端数据交互和状态管理。

---

## 🔐 1. 用户登录接口

### 接口信息
- **URL**: `POST /api/v1/user/login`
- **描述**: 验证用户名和密码，登录成功后返回 userId 及角色信息

### 请求体
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 登录用户名 |
| password | String | 是 | 登录密码（建议传输前做哈希处理） |

### 响应示例
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 2054492984730910700,
    "username": "zhangsan",
    "realName": "张三",
    "age": 65,
    "allergyHistory": "青霉素过敏",
    "chronicDiseases": "高血压",
    "role": "elder"
  }
}
```

### 后端实现
- **Controller**: [UserController.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/UserController.java#L33-L40)
- **Service**: [UserServiceImpl.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/UserServiceImpl.java#L60-L83)
- **Response DTO**: [UserLoginResponse.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/dto/UserLoginResponse.java)

---

## 👤 2. 获取用户档案接口

### 接口信息
- **URL**: `GET /api/v1/user/profile?userId={userId}`
- **描述**: 根据 userId 查询用户档案信息

### 请求参数
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | Long | Query | 是 | 用户ID（雪花算法生成） |

### 响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 2054492984730910700,
    "realName": "张三",
    "age": 65,
    "allergyHistory": "青霉素过敏",
    "chronicDiseases": "高血压",
    "role": "elder"
  }
}
```

### 后端实现
- **Controller**: [UserController.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/UserController.java#L46-L58)
- **Service**: [UserServiceImpl.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/UserServiceImpl.java#L85-L100)

---

## ✏️ 3. 更新用户档案接口

### 接口信息
- **URL**: `PUT /api/v1/user/profile?userId={userId}`
- **描述**: 更新用户的过敏史和慢性病史信息

### 请求参数
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | Long | Query | 是 | 用户ID（雪花算法生成） |

### 请求体
```json
{
  "allergyHistory": "青霉素过敏、海鲜过敏",
  "chronicDiseases": "高血压、糖尿病"
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| allergyHistory | String | 否 | 过敏史描述 |
| chronicDiseases | String | 否 | 慢性病史描述 |

### 响应示例
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

### 后端实现
- **Controller**: [UserController.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/UserController.java#L60-L74)
- **Service**: [UserServiceImpl.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/UserServiceImpl.java#L102-L124)

---

## 🎯 前端实现

### 1. 登录流程

#### Login 组件
文件: [Login.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/Login.js)

```javascript
// 登录成功后，将后端返回的数据传递给父组件
const response = await fetch('/api/v1/user/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});

const data = await response.json();
if (response.ok && data.code === 200) {
  onLogin(data.data); // 传递完整的用户信息
}
```

#### App 组件处理登录
文件: [App.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/App.js#L61-L85)

```javascript
const handleLogin = (loginData) => {
  // 保存完整的用户信息
  const userData = {
    userId: loginData.userId,        // 雪花算法生成的用户ID
    username: loginData.username,
    realName: loginData.realName,
    age: loginData.age,
    allergyHistory: loginData.allergyHistory,
    chronicDiseases: loginData.chronicDiseases,
    role: loginData.role
  };
  
  setUser(userData);
  setIsLoggedIn(true);
  
  // 保存到 localStorage（持久化存储）
  localStorage.setItem('currentUser', JSON.stringify(userData));
};
```

### 2. 页面刷新后恢复登录状态

文件: [App.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/App.js#L10-L18)

```javascript
// 从 localStorage 恢复登录状态
const [isLoggedIn, setIsLoggedIn] = useState(() => {
  const savedUser = localStorage.getItem('currentUser');
  return !!savedUser;
});

// 从 localStorage 恢复用户信息
const [user, setUser] = useState(() => {
  const savedUser = localStorage.getItem('currentUser');
  return savedUser ? JSON.parse(savedUser) : null;
});
```

### 3. 使用 userId 调用其他接口

#### ProfileModal 组件
文件: [ProfileModal.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/ProfileModal.js#L43-L52)

```javascript
// 使用 user.userId 作为 Query 参数
const response = await fetch(`/api/v1/user/profile?userId=${userId}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    allergyHistory: allergyHistory || null,
    chronicDiseases: chronicDiseases || null
  })
});
```

#### ProfileEdit 组件
文件: [ProfileEdit.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/ProfileEdit.js#L24-L33)

```javascript
// 同样使用 user.userId
const response = await fetch(`/api/v1/user/profile?userId=${user.userId}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    allergyHistory: allergyHistory || null,
    chronicDiseases: chronicDiseases || null
  })
});
```

### 4. 退出登录

文件: [App.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/App.js#L113-L120)

```javascript
const handleLogout = () => {
  setIsLoggedIn(false);
  setUser(null);
  setShowProfileModal(false);
  setActiveTab('home');
  // 清除 localStorage 中的用户信息
  localStorage.removeItem('currentUser');
};
```

---

## 🔄 完整流程图

```
┌─────────────┐
│  用户登录   │
└──────┬──────┘
       │ POST /api/v1/user/login
       ▼
┌─────────────────┐
│  后端验证身份   │
└──────┬──────────┘
       │ 返回 userId + 用户信息
       ▼
┌──────────────────┐
│ 前端接收并保存   │
│ - setState       │
│ - localStorage   │
└──────┬───────────┘
       │
       ├─────────────────────────────────────┐
       │                                     │
       ▼                                     ▼
┌──────────────┐                   ┌─────────────────┐
│  GET 查询档案 │                   │  PUT 更新档案   │
│  ?userId=xxx │                   │  ?userId=xxx    │
└──────┬───────┘                   └────────┬────────┘
       │                                    │
       └────────────────────────────────────┘
                    │
                    ▼
          ┌─────────────────┐
          │  使用 userId     │
          │  作为Query参数   │
          └─────────────────┘
```

---

## 📊 数据结构

### 数据库表结构
文件: [schema.sql](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/resources/schema.sql#L18-L33)

```sql
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `user_id` bigint NOT NULL UNIQUE COMMENT '用户ID（雪花算法生成）',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名/称呼',
  `age` tinyint NULL COMMENT '年龄',
  `allergy_history` text NULL COMMENT '过敏史描述',
  `chronic_diseases` text NULL COMMENT '慢性病史描述',
  `role` varchar(20) NOT NULL DEFAULT 'elder' COMMENT '角色',
  ...
)
```

### 双ID架构说明

| 字段 | 类型 | 用途 | 生成方式 |
|------|------|------|----------|
| id | bigint | 数据库内部主键，用于外键关联 | AUTO_INCREMENT |
| user_id | bigint | 对外暴露的用户标识，用于API接口 | 雪花算法 |

**为什么使用双ID？**
1. **安全性**: 不暴露数据库内部结构
2. **分布式友好**: 雪花算法ID可在分布式系统中唯一生成
3. **性能**: 自增ID作为聚簇索引，提升数据库性能
4. **灵活性**: 可迁移、可分库分表

---

## ⚠️ 注意事项

### 前端
1. **必须保存 userId**: 登录后必须将 `userId` 保存到 localStorage
2. **所有查询/修改接口**: 都必须携带 `userId` 作为 Query 参数
3. **页面刷新**: 从 localStorage 恢复用户信息，避免重新登录
4. **退出登录**: 必须清除 localStorage 中的用户信息

### 后端
1. **userId 格式**: 使用 String 接收 Query 参数，避免精度丢失
2. **查询条件**: 使用 `user_id` 字段而非 `id` 字段进行查询
3. **注册时生成**: 必须在注册时使用雪花算法生成 `user_id`
4. **返回数据**: 所有接口返回的必须是 `user_id` 而非 `id`

---

## 🧪 测试步骤

### 1. 注册用户
```bash
curl -X POST http://localhost:8080/api/v1/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "realName": "测试用户",
    "age": 65
  }'
```

### 2. 用户登录
```bash
curl -X POST http://localhost:8080/api/v1/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

响应中会包含 `userId`，例如: `2054492984730910700`

### 3. 查询用户档案
```bash
curl -X GET "http://localhost:8080/api/v1/user/profile?userId=2054492984730910700"
```

### 4. 更新用户档案
```bash
curl -X PUT "http://localhost:8080/api/v1/user/profile?userId=2054492984730910700" \
  -H "Content-Type: application/json" \
  -d '{
    "allergyHistory": "青霉素过敏",
    "chronicDiseases": "高血压"
  }'
```

---

## 📝 修改记录

| 日期 | 版本 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 2026-05-13 | 1.0 | 初始版本，实现双ID架构和登录流程 | AI Assistant |

---

## 🔗 相关文件

### 后端
- [UserController.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/UserController.java)
- [UserServiceImpl.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/UserServiceImpl.java)
- [UserLoginResponse.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/dto/UserLoginResponse.java)
- [UserProfileUpdateRequest.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/dto/UserProfileUpdateRequest.java)
- [SysUser.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/entity/SysUser.java)
- [BaseEntity.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/model/entity/BaseEntity.java)
- [SnowflakeIdGenerator.java](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/java/com/example/backend/common/util/SnowflakeIdGenerator.java)

### 前端
- [App.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/App.js)
- [Login.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/Login.js)
- [ProfileModal.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/ProfileModal.js)
- [ProfileEdit.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/components/ProfileEdit.js)
- [setupProxy.js](file:///D:/JAVAAAA/innovative-ideas-challenge/frontend/src/setupProxy.js)

### 数据库
- [schema.sql](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/resources/schema.sql)
- [migration_add_user_id.sql](file:///D:/JAVAAAA/innovative-ideas-challenge/backend/src/main/resources/migration_add_user_id.sql)
