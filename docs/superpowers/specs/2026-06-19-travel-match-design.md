# 开始旅游 · 实时匹配搭子功能设计

日期：2026-06-19

## 概述

在 TabBar 中间新增一个突出大按钮"出发"，进入实时在线匹配功能。用户选择目标景点后加入匹配队列，系统将选择同一景点的两个用户配对，双方确认后进入专属旅途页面，地图上实时显示双方位置。

---

## 一、TabBar 改造

- 现有 4 个等宽 tab：首页 / 攻略 / 翻译 / 我的
- 改为 5 个：首页 / 攻略 / **[出发]** / 翻译 / 我的
- 中间"出发"按钮：圆形，橙色渐变，尺寸大于普通 tab，向上突出 TabBar 上边缘
- 点击用 `uni.navigateTo` 跳转到 `pages/match/match`（不是 switchTab）
- TabBar 组件增加 `match` 类型的特殊 slot，避免影响其余四个 tab 的布局

---

## 二、匹配页（pages/match/match.vue）

页面内维护一个 `step` 状态，在同一页切换三个阶段：

### 阶段 1 — 选景点
- 顶部搜索框，输入关键字实时过滤
- 列表展示 `GET /api/spots` 返回的所有景点（名称 + 地区）
- 点击高亮选中，底部"开始匹配"按钮激活

### 阶段 2 — 等待中
- 进入后：获取当前位置，连接 WebSocket，发送 `join` 消息
- 全屏显示：旋转动画 + 景点名称 + "正在寻找搭子…"
- "取消匹配"按钮：发送 `cancel`，断开 WS，回到阶段 1

### 阶段 3 — 匹配成功确认弹窗
- 收到服务端 `matched` 消息后弹出
- 显示：搭子昵称 + "发现搭子！"
- "确认出发"：发送 `confirm`，等待服务端 `confirmed`
- "取消"：发送 `cancel`，回到阶段 1
- 收到 `partnerCancelled`：提示"搭子取消了"，回到阶段 1
- 15 秒超时未收到 `confirmed`：自动取消，回到阶段 1
- 收到 `confirmed`：`uni.redirectTo` 跳转旅途页，携带参数（景点ID、景点名、搭子昵称）

---

## 三、旅途页（pages/trip/trip.vue）

- 上方地图：
  - 🔵 自己的位置标记
  - 🔴 搭子的位置标记
- 下方面板：景点名称、搭子昵称、与搭子的实时距离（Haversine 公式计算）
- 进入页面后立即连接 WebSocket（复用同一个 session token）
- 每 3 秒调用 `uni.getLocation` 并发送 `location` 消息
- 收到 `locationUpdate`：更新搭子标记位置
- 收到 `partnerLeft`：弹窗提示"搭子已离开旅途"，停止更新
- "结束旅途"按钮：发送 `leave`，断开 WS，返回首页

---

## 四、WebSocket 消息协议

连接地址：`ws://<host>/ws?token=<JWT>`

### 客户端 → 服务端

| type | payload | 说明 |
|------|---------|------|
| `join` | `{ spotId, latitude, longitude }` | 加入匹配队列 |
| `confirm` | `{}` | 确认搭子 |
| `cancel` | `{}` | 取消匹配或取消确认 |
| `location` | `{ latitude, longitude }` | 旅途中更新位置 |
| `leave` | `{}` | 结束旅途 |

### 服务端 → 客户端

| type | payload | 说明 |
|------|---------|------|
| `waiting` | `{}` | 已入队，等待中 |
| `matched` | `{ partnerNickname, spotName }` | 找到搭子 |
| `confirmed` | `{}` | 双方确认，可进入旅途 |
| `partnerCancelled` | `{}` | 搭子取消 |
| `locationUpdate` | `{ latitude, longitude }` | 搭子位置更新 |
| `partnerLeft` | `{}` | 搭子结束旅途 |

所有消息格式：`{ "type": "...", "payload": { ... } }`

---

## 五、后端架构

### 新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 新增文件

```
src/main/java/com/LHZ/TripMate/
  config/
    WebSocketConfig.java                   注册 /ws 端点，允许跨域
    WebSocketHandshakeInterceptor.java     握手时验证 JWT，将 openid 存入 session attributes
  controller/
    MatchWebSocketHandler.java             TextWebSocketHandler，路由所有消息类型
  service/
    MatchService.java                      匹配核心逻辑（内存）
  dto/match/
    WsMessage.java                         通用消息 { type, payload }
```

### MatchService 内存结构

```java
// 等待队列：景点ID → 等待中的 session
ConcurrentHashMap<Long, WebSocketSession> waitingQueue

// 已匹配对：sessionId → 对方 sessionId
ConcurrentHashMap<String, String> matchedPairs

// 确认状态：sessionId → Boolean
ConcurrentHashMap<String, Boolean> confirmMap

// sessionId → openid（用于日志/扩展）
ConcurrentHashMap<String, String> sessionOpenid
```

### Security 配置

`SecurityConfig` 加一条 `permitAll`：
```java
.requestMatchers("/ws/**").permitAll()
```
JWT 验证由 `WebSocketHandshakeInterceptor` 在握手阶段单独处理；握手失败则拒绝连接（返回 401）。

---

## 六、前端新增文件

```
frontend/
  pages/
    match/match.vue        匹配页
    trip/trip.vue          旅途页
  api/
    spot.ts                GET /api/spots 封装
    match.ts               WebSocket 连接管理（connect/send/disconnect/onMessage）
```

`pages.json` 新增两个页面路由。

---

## 七、不在本期范围内

- 多人匹配（本期仅 1v1）
- 匹配历史记录
- 旅途内聊天
- 服务端持久化匹配状态（内存即可）
