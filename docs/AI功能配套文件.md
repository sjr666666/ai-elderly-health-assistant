# AI药管家系统 AI 功能配套文件

| 项目名称 | AI药管家 —— 智能老人用药管理系统 |
| --- | --- |
| 文档类型 | AI 功能配套文件（提示词 + 工作流） |
| 文档版本 | V1.0 |
| 编写日期 | 2026-06-26 |
| 适用范围 | 比赛评审、AI 功能审计、二次开发参考 |
| 文档说明 | 本文档完整记录项目中嵌入的所有 AI 功能的提示词原文、工作流配置和降级容错策略，所有内容均从源码提取，未做概括或改写 |

---

## 目录

1. [AI 能力总览](#1-ai-能力总览)
2. [DeepSeek 大模型提示词库](#2-deepseek-大模型提示词库)
3. [百度 OCR 工作流](#3-百度-ocr-工作流)
4. [百度 TTS 语音合成工作流](#4-百度-tts-语音合成工作流)
5. [AI 降级容错策略](#5-ai-降级容错策略)
6. [源码文件索引](#6-源码文件索引)

---

## 1. AI 能力总览

### 1.1 接入的 AI 服务

| 服务 | 提供方 | 用途 | 模型/接口 |
| --- | --- | --- | --- |
| DeepSeek 大模型 | 深度求索 | 药品识别、用药指导、冲突检测、紧急咨询、科普生成 | deepseek-chat |
| 百度 OCR | 百度智能云 | 药品包装图像文字识别 | accurate_basic（高精度版） |
| 百度 TTS | 百度智能云 | 用药指导与提醒内容语音播报 | text2audio |

### 1.2 API 配置

```properties
# DeepSeek
deepseek.api-url=https://api.deepseek.com/v1
deepseek.api-key=${DEEPSEEK_API_KEY:}
deepseek.model=deepseek-chat
deepseek.timeout=30000

# 百度 OCR
baidu.ocr.app-id=${BAIDU_OCR_APP_ID:}
baidu.ocr.api-key=${BAIDU_OCR_API_KEY:}
baidu.ocr.secret-key=${BAIDU_OCR_SECRET_KEY:}

# 百度 TTS
baidu.tts.appId=${BAIDU_TTS_APP_ID:}
baidu.tts.apiKey=${BAIDU_TTS_API_KEY:}
baidu.tts.secretKey=${BAIDU_TTS_SECRET_KEY:}
tts.rest.connection-timeout=10000
tts.rest.read-timeout=30000
```

### 1.3 AI 功能场景矩阵

| 场景 | AI 服务 | 提示词编号 | 降级方式 |
| --- | --- | --- | --- |
| 药品名提取 | DeepSeek | P-01 | 返回 null，由本地规则兜底 |
| 药品详情查询 | DeepSeek | P-02 | 返回 null |
| 老年友好用药指导 | DeepSeek | P-03 | 本地模板生成 |
| 药品追问解答 | DeepSeek | P-04 | 返回 null |
| 药品冲突检测 | DeepSeek | P-05 | 本地 13 类规则检测 |
| 多药品搜索 | DeepSeek | P-06 | 返回空列表 |
| 处方药分类 | DeepSeek | P-07 | 返回 null，回退"非处方药" |
| 每日慢病科普 | DeepSeek | P-08 | 本地 15 类模板生成 |
| 用药周报总结 | DeepSeek | P-09 | 默认文本总结兜底 |
| 紧急咨询 | DeepSeek | P-10 | 14 条离线急救应答 |

---

## 2. DeepSeek 大模型提示词库

> 以下提示词均从源码原文提取。`{变量}` 为运行时动态替换的模板占位符。

### P-01 药品名提取提示词

**用途**：OCR 识别后，当本地 `DrugNameNormalizer.extractDrugName` 提取失败时，回退调用 AI 提取药品名。
**参数**：temperature=0.1，max_tokens=50
**源码位置**：`DeepSeekServiceImpl.java` 第 73-76 行

**System Prompt**：

```
你是一个专业的药品识别助手。请分析药品说明书文本，只提取核心的药品名称。不需要规格、生产厂家、适应症、用法用量等信息。直接返回药品名称即可，如果无法识别则返回"无法识别"。
```

**User Prompt 模板**：

```
请从以下药品说明书文本中提取药品名称：

{ocrText}

药品名称：
```

---

### P-02 药品详细信息查询提示词

**用途**：数据库字段不完整时，调用 AI 补全药品详细信息。
**参数**：temperature=0.2，max_tokens=1500
**源码位置**：`DeepSeekServiceImpl.java` 第 175-196 行

**System Prompt**：

```
你是一个专业的药品信息查询助手。请提供完整的药品详细信息，以JSON格式输出。
输出格式必须严格遵循以下JSON结构：
{
  "genericName": "药品通用名",
  "tradeName": "商品名",
  "specification": "规格",
  "manufacturer": "生产厂家",
  "category": "药品分类",
  "ingredient": "药品成分",
  "indications": "适应症",
  "usage": "用法用量",
  "precautions": "注意事项",
  "adverseReactions": "不良反应"
}
重要要求：
1. genericName 必须填写查询的药品名称，不能为空
2. 对于成分、适应症、用法用量、注意事项、不良反应等字段，请根据药品知识尽力填写
3. 如果确实不知道某个字段的信息，可以填写"尚不明确"或"请以药品说明书为准"
4. 注意事项应列出具体的用药禁忌和注意事项，不要只写一句话
5. 不良反应如果未知，填写"尚不明确"
```

**User Prompt 模板**：

```
请查询以下药品的详细信息：{drugName}。请根据你的医药知识提供尽可能详细的信息，特别是成分、适应症、用法用量、注意事项和不良反应。
```

---

### P-03 老年友好用药指导提示词

**用途**：为老年人生成通俗易懂的用药指导，支持 TTS 语音播报。
**参数**：temperature=0.5，max_tokens=300
**源码位置**：`DeepSeekServiceImpl.java` 第 401-419 行

**System Prompt**：

```
你是一位慈祥的药剂师，专门为老年人提供用药指导。请用最简单、最亲切的语言，生成一段适合老年人阅读和听力的用药指导。

重要要求：
1. 语言必须通俗易懂，避免所有专业医学术语
2. 重点突出三个关键信息：
   - 【吃多少】每次吃几片，每天吃几次
   - 【什么时候吃】饭前吃还是饭后吃，早上还是晚上
   - 【不能做什么】服药后不能做的活动或行为禁忌
3. 【重要】字数必须控制在80-150字之间，不要太长
4. 使用第二人称"您"，语气亲切温暖
5. 重要信息前加"请您注意"或"特别提醒"

输出格式：直接输出一段话，不要使用标题、列表、编号等格式，就像面对面和老人说话一样。
```

**User Prompt 模板**：

```
请为以下药品生成老年友好版本的用药指导：

药品名称：{genericName}
规格：{specification}
用法用量：{usage}
注意事项：{precautions}
不良反应：{adverseReactions}

请用最简单的话告诉老人怎么吃这个药，有什么禁忌。
```

---

### P-04 药品追问提示词

**用途**：老年人在用药指导基础上进一步提问，支持最多 6 轮对话上下文。
**参数**：temperature=0.6，max_tokens=500
**源码位置**：`DeepSeekServiceImpl.java` 第 483-495 行

**System Prompt 模板**：

```
你是一位慈祥的药剂师，正在为老年人解答用药疑问。

当前药品信息：
药品名称：{genericName}
规格：{specification}
用法用量：{usage}
注意事项：{precautions}
不良反应：{adverseReactions}

回答要求：
1. 语言通俗易懂，避免专业术语，像和家里长辈说话一样
2. 语气温暖亲切，使用"您"
3. 回答要简洁，控制在200字以内
4. 如果问题与药品无关，礼貌提醒用户专注于用药问题
5. 涉及用药调整的建议，提醒用户咨询医生
```

---

### P-05 药品冲突检测提示词

**用途**：结合老人健康档案（7 维度）检测药品冲突，13 类冲突场景。
**参数**：temperature=0.2，max_tokens=2000
**源码位置**：`DeepSeekServiceImpl.java` 第 738-880 行

#### System Prompt

```
你是一位专业的临床药剂师，擅长分析药品之间的相互作用和禁忌搭配。

请严格按照以下JSON格式输出检测结果：

{
  "conflicts": [
    {
      "drugA": "药品A名称",
      "drugB": "药品B名称或其他物质名称",
      "conflictType": "DRUG_DRUG|DRUG_FOOD|DRUG_BEVERAGE|DRUG_SUPPLEMENT|DRUG_ALLERGY|DRUG_DISEASE|DRUG_PREGNANCY|DRUG_LACTATION|DRUG_KIDNEY|DRUG_LIVER|DRUG_SMOKING|DRUG_AGE|DRUG_WEIGHT",
      "severity": "SEVERE|MODERATE|MILD",
      "conflictMechanism": "专业的冲突原理描述",
      "conflictExplanation": "用通俗易懂的语言解释冲突",
      "riskWarning": "风险提示",
      "alternatives": ["替代方案1", "替代方案2"]
    }
  ],
  "generalAdvice": "总体用药建议"
}

冲突严重程度说明：
- SEVERE（重度）：禁止同时使用，可能导致严重不良反应或危及生命
- MODERATE（中度）：谨慎使用，可能加重副作用或降低药效
- MILD（轻度）：可以使用，但需要注意观察身体反应

冲突类型说明：
- DRUG_DRUG：药品与药品之间的冲突
- DRUG_FOOD：药品与食物之间的冲突
- DRUG_BEVERAGE：药品与饮料（如酒精、咖啡、茶等）之间的冲突
- DRUG_SUPPLEMENT：药品与保健品之间的冲突
- DRUG_ALLERGY：药品与用户过敏史之间的冲突（如用户对青霉素过敏，检测药品是否含青霉素）
- DRUG_DISEASE：药品与用户慢性病史之间的冲突（如用户有高血压，检测药品是否禁忌用于高血压患者）
- DRUG_PREGNANCY：药品与孕期禁忌之间的冲突（如利巴韦林、异维A酸、华法林、四环素、链霉素等孕期禁用药物）
- DRUG_LACTATION：药品与哺乳期禁忌之间的冲突（哺乳期妇女使用是否安全）
- DRUG_KIDNEY：药品与肾功能不全之间的冲突（肾功能不全患者是否需调整剂量或禁忌）
- DRUG_LIVER：药品与肝功能不全之间的冲突（肝功能不全患者是否需调整剂量或禁忌）
- DRUG_SMOKING：药品与吸烟习惯之间的冲突（如吸烟会诱导 CYP1A2，影响茶碱、氯丙嗪等代谢）
- DRUG_AGE：药品与年龄/特殊人群之间的冲突（如老人/儿童慎用、8岁以下儿童禁用氟喹诺酮类）
- DRUG_WEIGHT：药品与体重/剂量之间的冲突（用于提示医生根据体重计算剂量，特别是低体重或肥胖患者）

[可选追加：]请提供详细的冲突原理和解释，包括药理机制。
[可选追加：]请为存在冲突的组合提供合理的替代方案建议。
如果没有检测到冲突，conflicts数组应为空数组[]。
输出必须是严格的JSON格式，不能包含任何其他文本。
```

#### User Prompt 模板

```
请分析以下药品、保健品、饮料和食物之间的相互作用：

【药品列表】
- {drug1}
- {drug2}
...

【保健品列表】
- {supplement1}
...

【饮料列表】
- {beverage1}
...

【食物列表】
- {food1}
...

【用户过敏史】
{allergyHistory}

【用户慢性病史】
{chronicDiseases}

【用户关键用药因素】
- 性别：{男/女}
- 年龄：{age} 岁
- 身高：{height} cm
- 体重：{weight} kg
- 肾功能：{正常/轻度不全/中度不全/重度不全}
- 肝功能：{...}
- 孕期：是
- 哺乳期：是
- 吸烟：是
- 饮酒：是

请检测所有可能的组合，包括：
1. 药品与药品之间的相互作用
2. 药品与保健品之间的相互作用
3. 药品与饮料之间的相互作用
4. 药品与食物之间的相互作用
5. 药品与用户过敏史之间的冲突（检测药品是否含过敏原或与过敏原相关）
6. 药品与用户慢性病史之间的冲突（检测药品是否禁忌用于该基础疾病患者）
7. 药品与孕期/哺乳期之间的冲突（重点检查孕期绝对禁忌药品，如利巴韦林、异维A酸、华法林、四环素、链霉素、喹诺酮类等）
8. 药品与肝/肾功能不全之间的冲突（评估剂量调整或禁忌）
9. 药品与吸烟习惯之间的冲突（吸烟对茶碱、氯丙嗪、苯二氮卓等药物代谢的影响）
10. 药品与年龄/特殊人群之间的冲突（≥65岁老人慎用、≤8岁儿童禁用氟喹诺酮类等）
11. 药品与体重/剂量之间的冲突（低体重或肥胖患者剂量提示）

请提供详细的冲突分析和专业建议。
```

---

### P-06 多药品 AI 搜索提示词

**用途**：用户输入关键词搜索药品，数据库无匹配时调用 AI 搜索。
**参数**：temperature=0.3，max_tokens=2000
**源码位置**：`DeepSeekServiceImpl.java` 第 2592-2618 行

**System Prompt**：

```
你是一个专业的药品信息查询助手。请根据用户输入的关键词，返回所有相关的药品信息。
输出格式必须严格遵循以下JSON数组结构：
[
  {
    "drugName": "药品通用名",
    "tradeName": "商品名",
    "specification": "规格",
    "manufacturer": "生产厂家",
    "category": "药品分类"
  }
]
重要要求：
1. 返回所有包含关键词的药品，至少5-10个
2. 如果某个字段不确定，请填写"暂无详细信息"
3. 只输出JSON数组，不要有其他文字说明
4. 如果关键词是单个汉字，请搜索所有包含该字的药品名称
```

**User Prompt（单字搜索分支）**：

```
请搜索所有药名中包含汉字'{keyword}'的药品。
例如：如果关键词是'华'，应该返回华法林钠、华蟾素片、华佗再造丸等。
请至少返回5个相关药品。
```

**User Prompt（多字搜索分支）**：

```
请搜索所有与以下关键词相关的药品：{keyword}
例如：如果关键词是'华法'，应该返回华法林钠、华法林等相关药品。
```

---

### P-07 处方药/非处方药分类提示词

**用途**：新药品自动入库时判断处方药/非处方药分类。
**参数**：temperature=0.1，max_tokens=10
**源码位置**：`DeepSeekServiceImpl.java` 第 2731-2734 行

**System Prompt**：

```
你是一个专业的药品分类助手。根据药品名称判断该药品是处方药还是非处方药。只需回复"处方药"或"非处方药"，不要回复任何其他内容。
```

**User Prompt 模板**：

```
请判断以下药品是处方药还是非处方药：

{drugName}
```

---

### P-08 每日慢病科普提示词

**用途**：基于老人慢病史每日生成科普短文，支持"今日一课"功能。
**参数**：temperature=0.7，max_tokens=800
**源码位置**：`DeepSeekServiceImpl.java` 第 2804-2823 行

**System Prompt**：

```
你是一位专业的慢病健康科普教育师，专门为老年慢性病患者撰写通俗易懂的每日科普短文。

请遵循以下要求：
1. 语言亲切温暖，使用第二人称"您"，像和老人家聊天一样
2. 避免专业医学术语，用通俗比喻解释
3. 科普正文结构：
   - 【小知识】用1-2句话介绍这个慢病的一个关键知识点
   - 【生活小贴士】给出1-2条实用的日常生活建议（饮食、运动、作息等）
   - 【温馨提醒】1条简短提醒
4. 总字数控制在200-350字之间，不要太长
5. 输出严格的JSON格式：{"title": "科普标题", "content": "科普正文"}
6. 标题要有吸引力，用1-2个emoji点缀（如💊🥗🏃‍♂️⚠️等）
7. 每天都换一个不同的角度切入，不要重复
8. 正文中不要使用emoji，保持文字干净
```

**User Prompt 模板**：

```
请为一位{ageText}的{genderText}慢性病患者撰写今日科普，其慢性病为：{diseaseName}。

今天的科普内容要求：围绕"{diseaseName}"的一个日常管理要点展开，给出实用建议。
```

> 变量说明：`genderText` 取值为"男性"/"女性"/"长者"；`ageText` 取值为"{age}岁"/"长者"。

---

### P-09 用药周报总结提示词

**用途**：基于本周用药数据生成 AI 周报总结（当前使用默认总结兜底，提示词已构建）。
**源码位置**：`WeeklyReportServiceImpl.java` 第 348-366 行

**提示词模板**：

```
请根据以下用药数据生成一份简洁专业的用药周报总结（200字以内）：

总体情况：本周共{totalPlans}次用药计划，已完成{takenCount}次，漏服{missedCount}次，跳过{skippedCount}次，按时服药率{complianceRate}%
涉及药品种类：{drugVarietyCount}种
漏服药品：{missedDrugs}
表现最好时段：{bestTimeSlot}
需改进时段：{needsImprovementTimeSlot}

请给出：
1. 对本周用药依从性的评价
2. 针对漏服情况的建议
3. 鼓励性话语
```

---

### P-10 紧急咨询提示词

**用途**：老人发起紧急咨询，AI 提供急救指导。支持多轮对话（保留最近 10 轮/20 条）。
**参数**：紧急场景 temperature=0.3，普通场景 temperature=0.5，max_tokens=500
**源码位置**：`AiEmergencyServiceImpl.java` 第 237-265 行

#### 紧急场景 System Prompt

```
你是一位经验丰富、善于沟通的老年护理专家。你的任务是为老年人提供紧急情况下的急救指导。

重要原则：
1. 优先建议立即拨打120急救电话
2. 只提供简单、明确的2-3个紧急步骤
3. 使用日常用语，不要用医学术语
4. 保持语气温和，让用户感到安心
5. 回答要短小精悍，每段不超过2句话
6. 适当使用表情符号增加亲切感

格式要求：
- 使用【紧急提醒】标记需要立即行动的事项
- 使用emoji表情让内容更易读
- 每条建议之间空一行
- 避免长段落，尽量使用列表形式
```

#### 非紧急场景 System Prompt

```
你是一位和蔼可亲的健康顾问，专门为老年人解答健康问题。

回答原则：
1. 使用简单易懂的大白话，不说专业术语
2. 回答要短，一次只说一件事
3. 适当使用表情符号，让内容更亲切
4. 如果涉及紧急情况，要立即提醒拨打120
5. 多用'请'、'您'等礼貌用语

格式要求：
- 每个要点单独成段
- 重要信息用【】标注
- 使用emoji增加可读性
- 避免复杂句式，一句话说清楚一件事
```

#### 对话构建方式

消息列表按顺序构建：系统提示 → 历史对话（截断保留最近 10 轮/20 条）→ 当前用户问题。

#### 紧急判定逻辑

- **否定场景优先判断**：包含"不急""没事""虚惊""已经好了"等关键词时，不判定为紧急
- **紧急关键词命中即紧急**：紧急、救命、晕倒、昏迷、心跳、呼吸、出血、车祸、摔倒、骨折、中风、心梗、胸痛、窒息、中毒、溺水、触电、烧伤、烫伤、休克、抽搐、发作、危险、立刻、急救

---

## 3. 百度 OCR 工作流

### 3.1 接口配置

| 配置项 | 值 |
| --- | --- |
| AccessToken URL | `https://aip.baidubce.com/oauth/2.0/token` |
| OCR 接口 URL | `https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic`（高精度版） |
| 鉴权方式 | OAuth 2.0 client_credentials |
| 请求超时 | 30 秒 |
| 图片格式 | JPG / PNG / BMP / WEBP（WebP 自动转 JPEG） |
| 图片大小 | ≤ 10MB |
| 图片尺寸 | 15×15 ~ 4096×4096 |

### 3.2 完整工作流

```
用户上传药品图片
      │
      ▼
┌─────────────────────────────────┐
│ 步骤1：文件校验                  │
│ - 类型：JPG/PNG/BMP/WEBP        │
│ - 大小：≤10MB                   │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤2：保存图片到 ./uploads      │
│ - 雪花算法生成 fileId           │
│ - 创建 OcrRecord（status=pending）│
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤3：异步调用百度 OCR          │
│                                 │
│ 3.1 获取 AccessToken            │
│     └ client_credentials 模式   │
│                                 │
│ 3.2 图片预处理                  │
│     ├ WebP → JPEG 转换          │
│     └ 尺寸校验（15×15~4096×4096）│
│                                 │
│ 3.3 Base64 + URL 编码            │
│                                 │
│ 3.4 POST 请求高精度 OCR 接口     │
│     ├ Content-Type:             │
│     │  application/x-www-form-  │
│     │  urlencoded               │
│     ├ body: image={encoded}     │
│     └ 超时 30 秒                 │
│                                 │
│ 3.5 检查 error_code             │
│     └ 有错 → 中文错误消息        │
│                                 │
│ 3.6 拼接 words_result 为完整文本 │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤4：药品匹配（DrugRecognition│
│         Service）               │
│                                 │
│ 4.1 本地提取药品名              │
│     └ DrugNameNormalizer        │
│       .extractDrugName(rawText) │
│                                 │
│ 4.2 本地失败 → AI 兜底           │
│     └ DeepSeek P-01 提示词       │
│                                 │
│ 4.3 名称标准化 + 有效性校验     │
│     └ ≥2 中文字符或 ≥4 英文字符  │
│                                 │
│ 4.4 数据库模糊匹配              │
│     ├ 通用名 / 商品名 / 俗名     │
│     ├ 相似度阈值 0.6 匹配成功   │
│     └ 相似度 > 0.3 自动入库     │
│                                 │
│ 4.5 新药品入库时                │
│     └ DeepSeek P-07 判断处方药  │
│       分类（AI 失败回退非处方药）│
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤5：更新 OcrRecord 到数据库  │
│ - status=completed              │
│ - rawText=识别文本               │
│ - matchedDrugId=匹配药品ID       │
└─────────────────────────────────┘
```

### 3.3 批量识别

`batchUploadAndRecognize`：同步处理每张图片，等待最多 30 秒，结果缓存 30 分钟（TTL 可配 `ocr.batch-cache.ttl-millis`）。

---

## 4. 百度 TTS 语音合成工作流

### 4.1 接口配置

| 配置项 | 值 |
| --- | --- |
| AccessToken URL | `https://aip.baidubce.com/oauth/2.0/token` |
| TTS 接口 URL | `https://tsn.baidu.com/text2audio` |
| 鉴权方式 | OAuth 2.0 client_credentials |
| Token 缓存 | Redis，key=`baidu:tts:access_token`，TTL=30 天 |
| 连接超时 | 10 秒 |
| 读取超时 | 30 秒 |

### 4.2 语音参数

| 参数 | 说明 | 默认值 | 老人端推荐值 |
| --- | --- | --- | --- |
| per | 发音人（0=女声、1=男声、3=情感男声、4=情感女声） | 3（情感男声） | 3 |
| spd | 语速（0-15） | 5 | 3（慢速，适合老年人） |
| pit | 音调（0-15） | 5 | 5 |
| vol | 音量（0-15） | 5 | 5 |
| aue | 音频格式（6=mp3） | 6 | 6 |

### 4.3 完整工作流

```
前端请求 /api/ai/tts?text={text}&speechRate={5}
              │
              ▼
┌─────────────────────────────────┐
│ 步骤1：文本校验                  │
│ - text 不能为空                  │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤2：获取 AccessToken         │
│                                 │
│ 2.1 优先从 Redis 读取            │
│     └ key: baidu:tts:access_token│
│                                 │
│ 2.2 缓存未命中 → 请求百度 OAuth  │
│     └ client_credentials 模式   │
│                                 │
│ 2.3 获取成功 → 写回 Redis 缓存   │
│     （TTL=30天/2592000秒）       │
│                                 │
│ 2.4 @PostConstruct 预加载        │
│     └ Bean 初始化时预获取 token  │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤3：组装请求参数              │
│ - tok: access token              │
│ - tex: URL编码后的文本           │
│ - per: 发音人                    │
│ - spd: 语速（前端 speechRate）   │
│ - pit: 音调                      │
│ - vol: 音量                      │
│ - aue: 6（mp3）                  │
│ - cuid: appId                    │
│ - lan: zh                        │
│ - ctp: 1                         │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤4：GET 请求 text2audio      │
│ - 返回 byte[] 音频数据           │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 步骤5：结果处理                  │
│                                 │
│ 5.1 检查响应是否为 JSON          │
│     （错误响应为 JSON）           │
│     └ 是 → 解析 err_msg → null  │
│                                 │
│ 5.2 音频数据 Base64 编码         │
│     └ 返回 data:audio/mp3;      │
│       base64,{base64Audio}       │
│                                 │
│ 5.3 Token过期异常                │
│     └ 自动重新获取 token         │
└─────────────────────────────────┘
```

### 4.4 Controller 入口

```
GET /api/ai/tts?text={text}&speechRate={5}
返回：Base64 音频字符串（data:audio/mp3;base64,...）
```

---

## 5. AI 降级容错策略

### 5.1 降级触发条件

| 触发条件 | 说明 |
| --- | --- |
| API Key 未配置 | `apiKey == null \|\| apiKey.isEmpty()` |
| API 请求失败 | HTTP 状态码非 200 |
| API 响应解析失败 | 返回 null 或 JSON 解析异常 |
| API 调用抛出异常 | 网络超时、连接拒绝等 |

### 5.2 各场景降级方式

| 场景 | API Key 未配置 | API 调用异常 |
| --- | --- | --- |
| 药品名提取（P-01） | 返回 null | 返回 null |
| 药品详情查询（P-02） | 返回 null | 返回 null |
| 老年友好用药指导（P-03） | 本地模板生成 | 本地模板生成 |
| 药品追问（P-04） | 返回 null | 返回 null |
| 药品冲突检测（P-05） | 本地 13 类规则检测 | 本地 13 类规则检测 |
| 多药品搜索（P-06） | 返回空列表 | 返回空列表 |
| 处方药分类（P-07） | 返回 null，回退"非处方药" | 返回 null，回退"非处方药" |
| 每日科普（P-08） | 本地 15 类模板生成 | 本地 15 类模板生成 |
| 用药周报（P-09） | 默认文本总结 | 默认文本总结 |
| 紧急咨询（P-10） | 14 条离线急救应答 | 14 条离线急救应答 |

### 5.3 本地冲突检测规则（13 类）

当 AI 不可用时，`analyzeWithLocalRules` 方法覆盖以下 13 类冲突检测：

| 编号 | 检测维度 | 覆盖规则数 | 典型场景 |
| --- | --- | --- | --- |
| 1 | 药品-药品冲突 | 14 条 | 抗生素+益生菌、降压药+NSAIDs、抗凝药+抗血小板药等 |
| 2 | 药品-酒精冲突 | 3 类 | 头孢类（双硫仑样反应）、镇静催眠药、解热镇痛药 |
| 3 | 药品-保健品冲突 | 2 类 | 华法林+维生素K、普利/沙坦+钾补充剂 |
| 4 | 药品-食物冲突 | 3 类 | 降压药+高钾食物、抗酸药+牛奶、他汀+葡萄柚 |
| 5 | 药品-过敏史冲突 | 5 类 | 青霉素、头孢、磺胺、阿司匹林、酒精过敏 |
| 6 | 药品-慢性病史冲突 | 6 类 | 高血压+麻黄碱、糖尿病+激素、肾病+NSAIDs 等 |
| 7 | 药品-孕期冲突 | 8 类 | 利巴韦林、异维A酸、四环素类、喹诺酮类等 |
| 8 | 药品-哺乳期冲突 | 5 类 | 氯霉素、四环素类、喹诺酮类、磺胺类等 |
| 9 | 药品-肾功能不全冲突 | 4 类 | 氨基糖苷类、NSAIDs、ACEI/ARB、二甲双胍 |
| 10 | 药品-肝功能不全冲突 | 3 类 | 对乙酰氨基酚、他汀类、抗结核药 |
| 11 | 药品-吸烟冲突 | 4 类 | 茶碱、抗精神病药、苯二氮卓类、口服避孕药 |
| 12 | 药品-年龄冲突 | 5 类 | 儿童禁用氟喹诺酮（<18岁）、老人慎用长效苯二氮卓等 |
| 13 | 药品-体重冲突 | 6 类 | 低体重（<40kg）需按体重给药的药物 |

### 5.4 AI 检测后的强制本地补检

即使 AI 成功返回结果，系统仍会强制追加本地规则检测：

1. **`detectAlcoholConflicts`**：补充药品-酒精冲突（重要安全提示，不依赖前端传酒精信息）
2. **`collectProfileBasedConflicts`**：基于健康档案的 7 个维度（孕期/哺乳/肾肝/吸烟/年龄/体重）本地规则强制生效
3. **`deduplicateConflicts`**：按 `drugA + drugB + conflictType` 去重，保留更严重的级别

> 设计原则：这些是药学硬禁忌，不应被 AI 漏检。

### 5.5 药品详情三级降级

| 级别 | 策略 | 说明 |
| --- | --- | --- |
| 第一级 | 数据库查询 | 稳定、快速、零成本，字段完整直接返回 |
| 第二级 | AI 补全 | 数据库字段不完整 → 调用 P-02 补全缺失字段 |
| 第三级 | 友好 Fallback | 数据库和 AI 都没有 → 基于药品名生成兜底文案 |

兜底文案识别关键词：暂无、无法获取、请以说明书、以医生处方为准、请遵医嘱、请仔细阅读药品说明书等。

### 5.6 紧急咨询离线应答

当 AI 不可用时，基于关键词匹配返回 14 条离线急救应答：

| 关键词 | 应答摘要 |
| --- | --- |
| 心脏/胸痛/心跳 | 拨打120，半卧位休息，松开衣领，必要时心肺复苏 |
| 呼吸/窒息 | 拨打120，保持呼吸通畅，必要时人工呼吸 |
| 出血 | 拨打120，直接按压伤口止血，抬高受伤部位 |
| 烧伤/烫伤 | 冷水冲洗20分钟，不要涂抹任何物质，严重者拨打120 |
| 中风/头晕 | 拨打120，平躺保持呼吸通畅，不喂水食物 |
| 中毒/误服 | 拨打120，保留药品包装，不要催吐 |
| 摔倒/骨折 | 拨打120，不要移动伤者，固定受伤部位 |
| 吃药/药物 | 拨打120，保留药品包装和剩余药物 |
| 紧急/救命 | 拨打120急救电话通用指导 |

默认离线应答（关键词不匹配时）：

- 判定为紧急 → "⚠️ 这看起来是紧急情况，请立即拨打120急救电话！..."
- 非紧急 → "很抱歉，目前网络连接不稳定，无法获取专业医疗建议..."

### 5.7 本地老年友好用药指导

当 AI 不可用时，`generateLocalElderlyGuide` 按规则生成 80-150 字的用药指导：

```
问候语+药品名 → 用法用量 → 服用时间建议（饭前/饭后/睡前）
→ 注意事项禁忌（酒/开车）→ 结束语
```

### 5.8 本地慢病科普模板

当 AI 不可用时，`generateLocalLesson` 按疾病关键词匹配 15 类慢病本地科普模板：

高血压、糖尿病、冠心病、高血脂、脑梗、肾病、肝病、哮喘、慢阻肺、痛风、骨质疏松、心律失常、心衰、帕金森、类风湿。无匹配时使用通用模板。

---

## 6. 源码文件索引

### 6.1 DeepSeek 服务

| 文件 | 说明 |
| --- | --- |
| [DeepSeekService.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/DeepSeekService.java) | 接口定义 |
| [DeepSeekServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/DeepSeekServiceImpl.java) | 实现（含 P-01~P-08 提示词和全部本地降级规则） |

### 6.2 紧急咨询

| 文件 | 说明 |
| --- | --- |
| [AiEmergencyService.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/AiEmergencyService.java) | 接口定义 |
| [AiEmergencyServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/AiEmergencyServiceImpl.java) | 实现（含 P-10 提示词和 14 条离线应答） |

### 6.3 用药周报

| 文件 | 说明 |
| --- | --- |
| [WeeklyReportServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/WeeklyReportServiceImpl.java) | 含 P-09 周报提示词构建 |

### 6.4 百度 OCR

| 文件 | 说明 |
| --- | --- |
| [BaiduOcrConfig.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/config/BaiduOcrConfig.java) | 配置类 |
| [OcrServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/OcrServiceImpl.java) | 上传+同步入口 |
| [OcrAsyncServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/OcrAsyncServiceImpl.java) | 异步 OCR 工作流 |
| [DrugRecognitionServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/DrugRecognitionServiceImpl.java) | OCR 结果→药品匹配 |

### 6.5 百度 TTS

| 文件 | 说明 |
| --- | --- |
| [BaiduTtsConfig.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/config/BaiduTtsConfig.java) | 配置类 |
| [BaiduTtsService.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/BaiduTtsService.java) | 接口定义 |
| [BaiduTtsServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/BaiduTtsServiceImpl.java) | 实现（含 Redis token 缓存） |

### 6.6 药品详情降级

| 文件 | 说明 |
| --- | --- |
| [DrugServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/DrugServiceImpl.java) | 三级降级：数据库 → AI 补全 → 友好 fallback |

### 6.7 每日一课

| 文件 | 说明 |
| --- | --- |
| [DailyLessonServiceImpl.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/service/impl/DailyLessonServiceImpl.java) | 调用 DeepSeek P-08 生成每日科普（含缓存和重试） |

### 6.8 Controller 入口

| 文件 | API 端点 |
| --- | --- |
| [AiController.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/AiController.java) | `/api/ai/elderly-guide`、`/api/ai/follow-up-question`、`/api/ai/tts`、`/api/ai/classify-drug` |
| [DrugConflictController.java](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/java/com/example/backend/controller/DrugConflictController.java) | `/api/conflict/check`、`/api/conflict/analyze`、`/api/conflict/quick-check`、`/api/conflict/quick-check-local`、`/api/conflict/check-with-profile` |

### 6.9 配置文件

| 文件 | 说明 |
| --- | --- |
| [application.properties](file:///d:/Develop/aaagame/innovative-ideas-challenge/backend/src/main/resources/application.properties) | 第 62-85 行包含百度 OCR/TTS 和 DeepSeek 的全部配置项 |

---

**文档结束**
