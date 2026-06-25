# 老人端"我的家属"功能实现计划

## Context

老人端目前只能看到自己的紧急联系人，但**无法查看有哪些家属绑定了自己**。家属端绑定老人后，老人端没有相应的反馈入口，导致老人不知道自己被谁监护。

渐进式提醒的 notify_family 阶段会通知绑定的家属（通过 `guardian_elder_relation` 表），但老人端缺少查看该绑定关系的能力，导致用户产生"通知家属是通知给谁了"的疑问。

本次需求：在老人端新增"我的家属"功能，展示已绑定老人的家属列表（姓名、电话、关系、最近活跃时间），让老人清楚知道自己的监护人是谁。

## 实现方案

### 后端（4个文件）

#### 1. 新建 DTO：`GuardianSummaryDTO.java`

路径：`backend/src/main/java/com/example/backend/model/dto/GuardianSummaryDTO.java`

参考 `ElderSummaryDTO.java` 的注解风格（`@Data @Builder @NoArgsConstructor @AllArgsConstructor`），字段：
- `Long guardianId` - 家属ID
- `String realName` - 家属姓名
- `String phone` - 电话
- `Integer age` - 年龄
- `String gender` - 性别
- `String relationType` - 与老人的关系（如"儿子"、"女儿"、"配偶"等，来自 `GuardianElderRelation.relationType`）
- `String lastActiveTime` - 最近活跃时间（格式化字符串，如"5分钟前"，复用 `GuardianServiceImpl.formatLastActiveTime`）

#### 2. Mapper 扩展：`GuardianElderRelationMapper.java` + XML

- 在 `GuardianElderRelationMapper.java` 接口添加：`List<GuardianElderRelation> findByElderId(@Param("elderId") Long elderId);`
- 在 `GuardianElderRelationMapper.xml` 添加对应 SQL（参考现有 `findByGuardianId` 的 SQL，仅将 `WHERE guardian_id` 改为 `WHERE elder_id`）

#### 3. Service 扩展：`GuardianService.java` + `GuardianServiceImpl.java`

- 在 `GuardianService` 接口添加：`List<GuardianSummaryDTO> getGuardianList(Long elderId);`
- 在 `GuardianServiceImpl` 实现，参考现有 `getElderList(guardianId)` 的批量查询模式：
  1. 调用 `guardianElderRelationMapper.findByElderId(elderId)` 查询所有 active 关系
  2. 收集 guardianIds，调用 `userMapper.selectBatchIds(guardianIds)` 批量查询家属信息
  3. 复用现有 `formatLastActiveTime(lastActiveTime, updatedAt, now)` 私有方法格式化活跃时间
  4. 组装 `GuardianSummaryDTO` 列表返回
- 复用现有 `@Slf4j @Service @RequiredArgsConstructor` 注解，无需新增依赖

#### 4. Controller 扩展：`GuardianController.java`

添加新接口，放在 `getElderList` 方法之后（保持家属相关接口聚集）：

```java
@GetMapping("/by-elder")
public ResponseResult<List<GuardianSummaryDTO>> getGuardianList(@RequestParam Long elderId) {
    log.info("老人查询已绑定家属列表 - elderId: {}", elderId);
    try {
        return ResponseResult.success(guardianService.getGuardianList(elderId));
    } catch (Exception e) {
        log.error("获取家属列表失败 - elderId: {}", elderId, e);
        return ResponseResult.fail("获取家属列表失败：" + e.getMessage());
    }
}
```

**路径**：`GET /api/v1/guardian/by-elder?elderId={elderId}`

> 说明：无需权限校验（老人查自己的家属，使用老人自己的 id 即可），与现有 `getElderList` 接口风格一致。

### 前端（2个文件）

#### 5. 新建组件：`MyGuardiansModal.js`

路径：`frontend/src/components/MyGuardiansModal.js`

参考 `EmergencyContacts.js` 的弹窗结构和样式模式：
- 道具（Props）：`guardians`（家属列表）、`onClose`（关闭回调）、`userId`（用户ID，用于 fetch）
- 内部状态：`loading`、`guardianList`、`error`
- `useEffect` 加载家属列表：`fetch('/api/v1/guardian/by-elder?elderId=' + userId)`，解析 `data.code === 200` 取 `data.data`
- 弹窗结构（BEM 命名前缀 `family-contacts-`）：
  - `.family-contacts-modal`（全屏遮罩）
  - `.family-contacts-content`（白色卡片，复用 `.emergency-contacts-content` 大部分样式）
  - `.family-contacts-header`（标题"我的家属" + 关闭按钮）
  - `.family-contacts-body`（家属列表）
  - 家属卡片：复用 `.contact-card` 样式，展示姓名、关系徽章、电话、年龄、性别图标、最近活跃时间
  - 空状态：复用 `.empty-state` 样式，提示"暂无家属绑定，请让家属在家属端搜索您的用户名进行绑定"
- 样式定义追加到 `App.css`（复用 `.contact-card`、`.btn-primary`、`.empty-state` 等通用类，仅新增 `family-contacts-*` 前缀的容器类）

> 说明：根据项目规则，禁止使用 `window.confirm`/`window.alert`，本组件为只读列表，无操作按钮，不涉及弹窗。家属绑定的发起方在家属端，老人端只做查看。

#### 6. 修改：`App.js`

- **导入**：在顶部 `import MyGuardiansModal from './components/MyGuardiansModal';`
- **State**：在现有 `showAddContact` state 附近添加 `const [showMyGuardians, setShowMyGuardians] = useState(false);`
- **Header 按钮**：在 `App.js` 行3204-3207 的"紧急联系人"按钮后，新增"我的家属"按钮：
  ```jsx
  <button className="header-btn guardian-btn" onClick={() => setShowMyGuardians(true)}>
    <span className="btn-icon">👨‍👩‍👧</span>
    <span className="btn-label">我的家属</span>
  </button>
  ```
- **弹窗渲染**：在行6110 `EmergencyContacts` 弹窗渲染后，新增 `MyGuardiansModal` 渲染：
  ```jsx
  {showMyGuardians && (
    <MyGuardiansModal
      onClose={() => setShowMyGuardians(false)}
      userId={user?.id}
    />
  )}
  ```

> 说明：使用 `user.id`（数据库主键）作为 `elderId` 参数，与现有 `loadEmergencyContacts(user.id)` 风格一致。

### 样式（1个文件）

#### 7. 修改：`App.css`

在 `.emergency-contacts-modal` 相关样式附近追加 `.family-contacts-modal` 系列样式：
- `.family-contacts-modal`：复用 `.emergency-contacts-modal` 的全屏遮罩样式
- `.family-contacts-content`：复用卡片样式（max-width: 600px，圆角，slideUp 动画）
- `.family-contacts-header` / `.family-contacts-body`：复用相应样式
- `.family-contacts-close`：圆形关闭按钮
- `.guardian-card`：家属卡片样式（复用 `.contact-card` 布局）
- `.guardian-relation-badge`：关系徽章（如"儿子"标签）
- `.guardian-info-row`：信息行（电话、年龄、最近活跃）
- `.guardian-btn`（header按钮）：复用 `.contact-btn` 样式，仅背景色微调

## 验证方式

### 端到端测试

1. **启动后端和前端服务**（已在运行）
2. **登录老人端**：使用 `石敬荣来了/123456`（id=7，已被张三绑定）
3. **打开"我的家属"弹窗**：点击 header 区"我的家属"按钮
4. **验证**：
   - 弹窗显示标题"我的家属"
   - 列表显示1条家属记录：张三，电话 13800000002，关系"子女"（或 guardian_elder_relation 表中实际存储的 relationType 值）
   - 最近活跃时间显示（如"3小时前"）
   - 空状态验证：可临时解绑后查看空状态提示（可选）
5. **接口直接验证**：`curl "http://localhost:8080/api/v1/guardian/by-elder?elderId=7"` 应返回 JSON，code=200，data 包含张三信息

### 测试后清理

测试通过后，根据用户偏好，无需保留测试文件（本次测试直接通过浏览器交互验证，不产生测试脚本文件）。

## 涉及文件清单

**新建**（2个）：
1. `backend/src/main/java/com/example/backend/model/dto/GuardianSummaryDTO.java`
2. `frontend/src/components/MyGuardiansModal.js`

**修改**（5个）：
3. `backend/src/main/java/com/example/backend/mapper/GuardianElderRelationMapper.java` - 添加 findByElderId 方法
4. `backend/src/main/resources/mapper/GuardianElderRelationMapper.xml` - 添加 SQL
5. `backend/src/main/java/com/example/backend/service/GuardianService.java` - 添加 getGuardianList 接口方法
6. `backend/src/main/java/com/example/backend/service/impl/GuardianServiceImpl.java` - 实现 getGuardianList
7. `backend/src/main/java/com/example/backend/controller/GuardianController.java` - 添加 /by-elder 接口
8. `frontend/src/App.js` - 导入组件、添加 state、header 按钮、弹窗渲染
9. `frontend/src/App.css` - 添加 family-contacts-* 样式

## 复用现有资源

- **DTO 注解风格**：参考 `ElderSummaryDTO.java`（`@Data @Builder @NoArgsConstructor @AllArgsConstructor`）
- **Mapper 批量查询模式**：参考 `GuardianServiceImpl.getElderList` 第64-154行
- **活跃时间格式化**：复用 `GuardianServiceImpl.formatLastActiveTime` 第457-473行
- **前端弹窗结构**：参考 `EmergencyContacts.js` 的 BEM 命名和 CSS 结构
- **前端样式**：复用 `.contact-card`、`.btn-primary`、`.empty-state` 等通用类
- **日志注解**：复用 `@Slf4j`（已在 GuardianServiceImpl 上）
