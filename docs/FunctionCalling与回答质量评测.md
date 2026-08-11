# Function Calling 工具调用 + 回答质量评测

> 本文讲解「AI 管家」能力的实现：从"只会问答"到"能办事"，以及如何量化回答质量、形成反馈闭环。适合面试讲"AI 应用落地"的完整故事线。

## 一、为什么做 Function Calling

### 现状诊断

原 AI 功能全部是"纯 prompt + 规则"：紧急助手回答急救问题、RAG 回答用药问题，**只说不做**。老人说"帮我看看药箱里有什么"，AI 只能礼貌地回答"请您到药箱页面查看"——这不是管家，是复读机。

### 目标

让 AI 能调用系统工具，真正办成四件事（与 README 开发计划对齐）：

| 工具名 | 触发场景 | 背后服务 |
|--------|----------|----------|
| `query_medicine_box` | "药箱里有什么药 / 我都在吃什么药" | `MedicineBoxService.getMedicineBoxList` |
| `create_medication_plan` | "帮我安排今天的吃药计划" | `PlanService.addBoxItemToMedicationPlan` + `generateDailyPlanFromMedicineBox` |
| `mark_dose_missed` | "早上忘了吃芬必得，标记漏服" | `PlanService.executeMedicationAction(planId, userId, "skip")` |
| `notify_guardian` | "通知我女儿，我今天头晕" | `SmsNotificationService.sendNotification` |

## 二、实现架构

```
老人提问 → DeepSeek(messages + tools) 
   ├─ 返回普通回答 → 直接展示
   └─ 返回 tool_calls → AiToolService.execute(userId, toolName, args)
        └─ 工具结果 JSON 回传 → 再次调用 DeepSeek → 归纳成大白话
```

### 关键代码路径

**1. 工具定义（OpenAI / DeepSeek 兼容 tools 格式）** — `AiToolServiceImpl.getToolDefinitions()`

```json
{"type":"function","function":{"name":"mark_dose_missed",
  "description":"把用户今天某个药品的服药计划标记为「漏服」...",
  "parameters":{"type":"object","properties":{"drugName":{"type":"string"},
    "timeSlot":{"type":"string","description":"morning/noon/evening/before_bed，用户说了时段必填"}},
    "required":["drugName"]}}}
```

**2. 多轮工具调用循环** — `AiEmergencyServiceImpl.callDeepSeekAI`

```java
for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {  // 最多 3 轮，防死循环
    // 1. 带 tools 调用 DeepSeek
    // 2. 若返回 message.tool_calls：
    //    - assistant 消息原样回传（含 tool_calls，API 要求）
    //    - 逐个执行 AiToolService.execute(userId, name, args)
    //    - tool 结果以 {role:"tool", tool_call_id, content} 追加
    //    - continue 下一轮
    // 3. 无 tool_calls → 提取 content 返回
}
```

### 三个关键设计点（面试可讲）

1. **参数由 LLM 生成，但执行权在自己手里**。`AiToolService.execute` 校验 userId（JWT 上下文，不信任前端传参）、工具名白名单、参数 JSON 解析失败兜底。LLM 只负责"决定调哪个工具、传什么参数"，数据安全边界在后端。

2. **多时段歧义 → 让 AI 追问而不是乱执行**。芬必得一天三次（morning/noon/evening），用户只说"忘了吃芬必得"时，工具返回"有多个时段，请指定"，AI 会反问"是早上那次吗"。用户说了"早上"（timeSlot=morning），工具精确定位。**宁可让 AI 确认，也不要标错漏服。**

3. **系统提示词里写清楚工具触发条件**（Few-shot 引导）：
   ```
   老人问「药箱里有什么药」 → 调用 query_medicine_box
   老人说「今天忘了吃XX药（早上/中午/晚上/睡前）」 → 调用 mark_dose_missed（传 timeSlot）
   ```
   实测 DeepSeek-chat 对中文工具名 + 场景描述的理解准确率很高，4 个工具全部一次识别成功。

### 实测效果（真实 DeepSeek Key）

- "帮我看看我的药箱里现在都有什么药" → 调用工具 → 「您的药箱里有2种药：💊阿莫西林 每次1片一天3次…」
- "帮我安排一下今天的吃药计划" → 生成 morning/noon/evening 三段计划 → 「您今天一共要吃两种药，每天三次…」
- "我早上忘了吃芬必得，帮我标记一下漏服" → timeSlot=morning 精确标记 → 「【芬必得】早上那次已经记成漏服了」
- "帮我通知我女儿，我今天头有点晕" → 发短信 → 「已通知您女儿张三，她会尽快联系您的」

## 三、回答质量评测集（20 题）

### 为什么需要

RAG 检索好不好，不能靠"感觉"。20 道覆盖药品/指南/FAQ 的代表性问题，每题标注期望命中的关键词，跑一遍统计命中率，**量化质检**。

### 使用

```bash
python scripts/rag-eval/evaluate_rag.py --base http://localhost:8081 --username laowang --password 123456
```

输出：每题 ✅/❌ + 检索模式（VECTOR/KEYWORD/LOCAL）+ 命中率汇总 + 低于 85% 时给出改进建议。`--json` 输出机器可读结果。

### 实测记录（这就是"评测驱动开发"的证据）

| 阶段 | 命中率 | 模式 | 问题 |
|------|--------|------|------|
| 旧向量（512 维 local-hash，未重灌） | 17/20 = 85% | 全 KEYWORD | 向量维度不匹配 → 检索全部降级关键词 |
| **重灌 bge-m3 后** | **20/20 = 100%** | 全 VECTOR | 中成药/失眠/心脏病 3 题语义命中（"安眠药能长期吃吗"→失眠、"冠心病"→心脏病） |

> 教训（README 已有铁律印证）：**换 embedding provider 后必须全量重灌**（`POST /api/rag/ingest`）。旧库向量维度与新 provider 不匹配，点积全错，检索会无声降级。

### 命中率判定的口径

`sources[].title 或 sourceRef 或 content` 包含任一期望关键词 → 命中。注意这是**检索质量**评测（资料是否召回对），不是**生成质量**评测（LLM 回答好不好）——后者靠反馈闭环。

## 四、反馈闭环（👍/👎）

### 链路

```
RagAskCard 回答下方「这个回答有用吗」
   → POST /api/rag/feedback {question, answer, rating(1|-1), mode}
   → rag_feedback 表落库（V7 迁移 + V9 补列）
   → 评测集校准 + 质量监控数据源
```

### 为什么 rating 用 1/-1 而不是 1-5

老人用户，星评是负担，两个大按钮最友好；落库后按 rating 分组统计（`SELECT rating, COUNT(*) ... GROUP BY rating`）即可得到"好评率"。question + mode 一起存，可以定位"哪个问题在哪个模式下回答不好"——比如"LOCAL 模式好评率低"说明降级链路该优化。

## 五、统一免责声明 + prompt 注入防护

### 统一免责声明

`SafetyGuard.DISCLAIMER`（"以上信息仅供参考，不能替代专业医生的诊断和治疗，具体用药请遵医嘱。如有紧急情况，请立即拨打120。"）：
- 系统提示词要求 LLM 回答末尾带上
- `appendDisclaimer()` 兜底：回答没带"遵医嘱"字样就自动追加（覆盖本地直出/流式降级等不走 LLM 的路径）

### prompt 注入防护（拦截式，不后置过滤）

`SafetyGuard.isSafe(input)` 在**调用 LLM 之前**检查：
- 注入攻击：忽略指令 / 泄露系统提示词（"忽略以上所有指令"、"reveal your system prompt"、越狱）
- 危险请求：索要致死剂量 / 自伤（"吃多少能死"、"自杀用什么药"）

命中 → 不调用 LLM，直接返回固定拒绝文案 + 引导就医，日志记录 `safetyCheckPassed=false`。RAG 路径返回 `mode=GUARDED` 便于前端区分。

**为什么拦截式比后置过滤好**：省一次 LLM 调用（成本 + 延迟）、杜绝"模型被诱导后输出再被拦"的窗口、危险请求根本不进入模型。

### 精确匹配的难点（面试可讲）

"这药会毒死人吗"是老人的正当担忧，**不能误杀**；"怎么用老鼠药毒死人"是恶意，**必须拦**。区别在意图词（怎么/如何/多少）+ 动作词（毒死/自杀）的组合，而非单个词。SafetyGuard 用双词组合正则实现，单测覆盖了 6 类正当问题不误杀 + 5 类恶意请求必拦截。

## 六、附带修复的存量 bug

测试 Function Calling 时发现 `sendNotification` 短信通知**一直存在**的 bug：手机号 AES 加密后 Base64 密文约 44 字符，`sms_notification_log.phone` 列只有 varchar(20) → 每次插入都 `Data too long`。漏服提醒/紧急通知等所有短信路径都会触发（只是定时任务被 try-catch 吞了错误）。

修复：**V8 迁移** `ALTER TABLE sms_notification_log MODIFY phone VARCHAR(128)`。这个 bug 是"功能没测到就用"的典型——AI 工具调用把隐藏路径激活了才暴露。

## 七、Q&A

**Q1：为什么不在所有 AI 功能都加 Function Calling，只在紧急助手？**
因为工具需要"对话场景"才有意义。紧急助手是老人最常聊天的入口，RAG 用药问问是单轮知识问答（有检索就有答案）。工具调用 + 多轮上下文是配套的——单轮问答里调工具会显得莫名其妙。

**Q2：工具执行结果返回给模型会不会泄露隐私？**
工具返回的是**要展示给老人本人的信息**（他自己的药箱/计划），不存在跨用户泄露。且 userId 从 JWT SecurityContext 取，工具内部校验归属（`addBoxItemToMedicationPlan` 会校验 boxItem 属于当前用户）。

**Q3：评测集 20 题会不会过拟合？**
会，这是它的局限。解法：20 题是"回归基线"（改检索/加知识后跑一遍防止退化），真正的质量靠 `rag_feedback` 持续收集真实用户反馈。基线 + 反馈双轨，就是工程上的"测试 + 监控"。

**Q4：MAX_TOOL_ROUNDS=3 会不会不够？**
演示场景单次工具调用足够（查药箱→回答）。3 轮可覆盖"查计划→标记漏服→通知家属"这种链路。上限是为了防 LLM 陷入工具调用死循环（成本失控），宁可多轮确认也不能无限调。
