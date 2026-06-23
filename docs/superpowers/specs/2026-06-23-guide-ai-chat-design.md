# 旅游导览数字人 AI 聊天接口设计

**日期：** 2026-06-23  
**范围：** Spring Boot 后端新增导览 AI 聊天模块  
**当前景点：** 西南大学（spot_key: `swu`）

---

## 一、背景与目标

在 TripMate 小程序中新增"导览数字人"功能，用户在景点页面可以与 AI 导游对话，获取景点介绍、历史文化、游览建议等信息。

**核心需求：**
- 按景点切换人格和背景知识（当前仅西南大学）
- 后端持久化对话历史，关闭小程序后可继续
- 流式（SSE）返回 AI 回复，前端呈现打字机效果
- 管理后台可编辑景点人设和知识库，无需改代码

**AI 提供商：** DeepSeek（兼容 OpenAI 格式，国内直连）

---

## 二、数据模型

### `guide_spot_config` — 景点导游配置

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 自增主键 |
| spot_key | VARCHAR(50) UNIQUE | 景点标识，如 `swu` |
| persona_name | VARCHAR(50) | 数字人名字，如"小渝" |
| persona_desc | TEXT | 人设描述（性格、语气、定位） |
| knowledge_text | TEXT | 自由文本知识库（历史、建筑、文化等） |
| active | BOOLEAN | 是否启用，默认 true |

### `guide_session` — 对话会话

每个用户对每个 spot_key 唯一一条，发消息时自动创建。

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT | 微信用户 ID |
| spot_key | VARCHAR(50) | 景点标识 |
| updated_at | DATETIME | 最后活跃时间 |

唯一约束：`(user_id, spot_key)`

### `guide_message` — 消息记录

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| session_id | BIGINT FK | 所属会话 |
| role | VARCHAR(10) | `USER` 或 `ASSISTANT` |
| content | TEXT | 消息内容 |
| created_at | DATETIME | 创建时间 |

上下文窗口：每次调用 AI 时取最近 20 条消息拼入 messages。

---

## 三、API 接口

### 小程序侧（需 WX_USER 登录）

#### `GET /api/guide/{spotKey}/history`
返回最近 20 条历史消息。

**响应：**
```json
{
  "code": 200,
  "data": [
    { "role": "USER", "content": "西南大学有哪些著名建筑？", "createdAt": "..." },
    { "role": "ASSISTANT", "content": "西南大学最著名的建筑是...", "createdAt": "..." }
  ]
}
```

#### `POST /api/guide/{spotKey}/chat`
发送消息，**SSE 流式返回** AI 回复。

**请求头：** `Accept: text/event-stream`  
**请求体：**
```json
{ "message": "西南大学有哪些著名建筑？" }
```

**SSE 响应流：**
```
data: {"delta":"西南"}
data: {"delta":"大学最"}
data: {"delta":"著名的建筑..."}
data: {"done":true}
```

前端收到 `done:true` 时，将拼接的完整回复渲染为最终消息。  
后端在流结束后将完整回复作为 `ASSISTANT` 消息写入数据库。

#### `DELETE /api/guide/{spotKey}/history`
清空当前用户在该景点的全部历史消息（保留 session 记录）。

---

### 管理后台侧（需 SUPER_ADMIN）

#### `GET /api/admin/guide/configs`
列出所有景点配置。

#### `PUT /api/admin/guide/configs/{spotKey}`
新建或更新景点配置（upsert）。

**请求体：**
```json
{
  "personaName": "小渝",
  "personaDesc": "西南大学的专属校园导览助手...",
  "knowledgeText": "西南大学创建于...",
  "active": true
}
```

---

## 四、DeepSeek 集成

### 配置（application.yaml）

```yaml
deepseek:
  api:
    url: https://api.deepseek.com/chat/completions
    key: sk-xxxxxxxx
    model: deepseek-chat
```

### 流式处理流程

```
前端 POST /api/guide/{spotKey}/chat
       ↓
GuideChatController 创建 SseEmitter（超时 120s）并返回
       ↓
GuideService（异步线程）：
  1. 查/建 GuideSession
  2. 保存 USER 消息到 guide_message
  3. 从 DB 取最近 20 条历史
  4. 拼装 DeepSeek messages（system + history + 本条）
  5. 调 DeepSeekClient（RestClient，stream=true）
       ↓
DeepSeekClient 逐行读取 SSE 响应：
  每收到一个 delta → emitter.send({"delta":"..."})
  流结束 → 拼接完整回复 → 回调 GuideService 保存 ASSISTANT 消息
          → emitter.send({"done":true}) → emitter.complete()
  异常  → emitter.send({"error":"..."}) → emitter.complete()
```

### 错误处理
- DeepSeek 调用超时（>120s）：推送 `{"error":"响应超时，请重试"}` 并关闭
- 网络异常：推送 `{"error":"网络异常"}` 并关闭
- 景点配置不存在或未启用：HTTP 400，正常 Result 响应

---

## 五、System Prompt 构建

```
你是{personaName}，{personaDesc}。

## 你掌握的景点知识
{knowledgeText}

## 对话规范
- 回答简洁自然，每次不超过 150 字
- 不知道的内容直接说"这个我还不太清楚"，不编造
- 保持导游视角，语气亲切，适当引导游客探索
- 使用中文回答
```

### 西南大学初始配置（DataInitializer 自动写入，已存在则跳过）

- `spot_key`: `swu`
- `persona_name`: `小渝`
- `persona_desc`: `西南大学的专属校园导览助手，熟悉学校的历史沿革、标志性建筑、校园文化和日常生活，性格温暖幽默，说话亲切自然`
- `knowledge_text`:
  > 西南大学创建于1906年，坐落于重庆市北碚区，是教育部直属的全国重点综合大学，国家"211工程"和"985工程优势学科创新平台"建设高校。校训为"含弘光大，继往开来"。主要地标建筑包括：含弘楼（行政办公主楼）、图书馆（馆藏丰富，建筑宏伟）、惟勤楼（重要教学楼）、博雅广场（师生日常集会场所）、荷花池（校园标志性景观）。学校拥有农学、教育学、心理学等优势学科，是重庆市重要的科研和人才培养基地。

---

## 六、新增后端代码结构

```
com.LHZ.TripMate
├── config/
│   └── DeepSeekConfig.java          # DeepSeek 配置属性
├── entity/
│   ├── GuideSpotConfig.java
│   ├── GuideSession.java
│   └── GuideMessage.java
├── repository/
│   ├── GuideSpotConfigRepository.java
│   ├── GuideSessionRepository.java
│   └── GuideMessageRepository.java
├── dto/
│   ├── GuideChatRequestDTO.java
│   ├── GuideMessageDTO.java
│   └── admin/GuideSpotConfigDTO.java
├── service/
│   ├── GuideService.java
│   └── DeepSeekClient.java
├── service/impl/
│   └── GuideServiceImpl.java
└── controller/
    ├── GuideChatController.java
    └── admin/AdminGuideController.java
```

`SecurityConfig` 新增规则：
- `GET/POST/DELETE /api/guide/**` → `hasRole('WX_USER')`
- `GET/PUT /api/admin/guide/**` → 已由 `@PreAuthorize("hasRole('SUPER_ADMIN')")` 覆盖

---

## 七、不在本期范围内

- 语音输入/输出（TTS/ASR）
- 多景点管理 UI（Admin 面板前端页面）
- 会话列表/历史归档
- 用量统计与 token 计费
