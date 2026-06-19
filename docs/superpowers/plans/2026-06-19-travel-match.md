# 开始旅游·实时匹配搭子 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** TabBar 中间增加"出发"大按钮，用户选景点后通过 WebSocket 实时匹配搭子，双方确认后进入旅途页面显示双方位置。

**Architecture:** 后端用 Spring WebSocket（TextWebSocketHandler）维护内存匹配队列；前端 uni-app 用 `uni.connectSocket` SocketTask API 管理 WS 连接；match.vue 三状态切换（选景点→等待→确认），trip.vue 显示双方地图位置。

**Tech Stack:** Spring Boot WebSocket、ConcurrentHashMap（内存匹配）、uni-app SocketTask、Vue 3 Composition API

---

## 文件结构

### 后端新增
```
src/main/java/com/LHZ/TripMate/
  dto/match/WsMessage.java
  config/WebSocketHandshakeInterceptor.java
  config/WebSocketConfig.java
  service/MatchService.java
  controller/MatchWebSocketHandler.java
```

### 后端修改
```
pom.xml                                     加 websocket 依赖
src/.../config/SecurityConfig.java          加 /ws/** permitAll
src/.../controller/ScenicSpotController.java  findAll() 改返回 Result<List<ScenicSpot>>
```

### 前端新增
```
frontend/api/spot.ts
frontend/api/match.ts
frontend/pages/match/match.vue
frontend/pages/trip/trip.vue
```

### 前端修改
```
frontend/pages.json
frontend/locales/zh.json
frontend/locales/en.json
frontend/components/TabBar/TabBar.vue
```

---

## Task 1: 后端 — 加 WebSocket 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 pom.xml 的 `<dependencies>` 末尾加入 websocket 依赖**

在 `</dependencies>` 之前插入：
```xml
        <!-- WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
```

- [ ] **Step 2: 验证编译通过**

```bash
./mvnw compile -q
```
Expected: 无报错输出（只有警告可以忽略）

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: 加入 spring-boot-starter-websocket 依赖"
```

---

## Task 2: 后端 — WsMessage DTO

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/dto/match/WsMessage.java`

- [ ] **Step 1: 创建文件**

```java
package com.LHZ.TripMate.dto.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private String type;
    private Object payload;

    public static WsMessage of(String type) {
        return new WsMessage(type, Map.of());
    }

    public static WsMessage of(String type, Object payload) {
        return new WsMessage(type, payload);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -q
```
Expected: 无报错

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/dto/match/WsMessage.java
git commit -m "feat: 添加 WsMessage DTO"
```

---

## Task 3: 后端 — WebSocketHandshakeInterceptor

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/config/WebSocketHandshakeInterceptor.java`

- [ ] **Step 1: 创建文件**

```java
package com.LHZ.TripMate.config;

import com.LHZ.TripMate.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String openid = jwtUtil.extractUsername(token);
            String userType = jwtUtil.extractUserType(token);
            if (openid == null || !"WX_USER".equals(userType)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put("openid", openid);
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -q
```
Expected: 无报错

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/config/WebSocketHandshakeInterceptor.java
git commit -m "feat: 添加 WebSocket 握手拦截器（JWT 验证）"
```

---

## Task 4: 后端 — WebSocketConfig

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/config/WebSocketConfig.java`

- [ ] **Step 1: 创建文件**

```java
package com.LHZ.TripMate.config;

import com.LHZ.TripMate.controller.MatchWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MatchWebSocketHandler matchWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(matchWebSocketHandler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./mvnw compile -q
```
Expected: 无报错（此时 MatchWebSocketHandler 还未创建，会报错——先创建 Task 5、6 再回来编译）

> 注意：Task 4 和 Task 5、6 存在循环依赖，可以先继续，在 Task 6 完成后统一编译。

- [ ] **Step 3: Commit（Task 6 完成后再 commit）**

---

## Task 5: 后端 — MatchService

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/MatchService.java`
- Test: `src/test/java/com/LHZ/TripMate/service/MatchServiceTest.java`

- [ ] **Step 1: 先写单元测试**

```java
package com.LHZ.TripMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService();
    }

    private WebSocketSession mockSession(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void firstUserJoins_getsEmptyPartner() {
        WebSocketSession s1 = mockSession("s1");
        Optional<WebSocketSession> result = matchService.tryMatch(s1, 1L);
        assertThat(result).isEmpty();
    }

    @Test
    void secondUserJoins_sameSpot_bothGetMatched() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 1L);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("s1");
    }

    @Test
    void secondUserJoins_differentSpot_noMatch() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 2L);
        assertThat(result).isEmpty();
    }

    @Test
    void getPartner_afterMatch_returnsPartner() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        assertThat(matchService.getPartner("s1")).isPresent();
        assertThat(matchService.getPartner("s2")).isPresent();
    }

    @Test
    void confirm_onlyOneConfirmed_returnsFalse() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        assertThat(matchService.confirm("s1")).isFalse();
    }

    @Test
    void confirm_bothConfirmed_returnsTrue() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.tryMatch(s2, 1L);
        matchService.confirm("s1");
        assertThat(matchService.confirm("s2")).isTrue();
    }

    @Test
    void cancel_removesFromQueue() {
        WebSocketSession s1 = mockSession("s1");
        WebSocketSession s2 = mockSession("s2");
        matchService.tryMatch(s1, 1L);
        matchService.cancel(s1);
        // s2 进来应进入等待而不是匹配 s1
        Optional<WebSocketSession> result = matchService.tryMatch(s2, 1L);
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败（类不存在）**

```bash
./mvnw test -pl . -Dtest=MatchServiceTest -q 2>&1 | tail -5
```
Expected: 编译错误 `cannot find symbol: class MatchService`

- [ ] **Step 3: 创建 MatchService**

```java
package com.LHZ.TripMate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MatchService {

    // 等待队列：景点ID → 等待中的 session
    private final ConcurrentHashMap<Long, WebSocketSession> waitingQueue = new ConcurrentHashMap<>();

    // 已匹配对：sessionId → 对方 session
    private final ConcurrentHashMap<String, WebSocketSession> matchedPairs = new ConcurrentHashMap<>();

    // 确认状态：sessionId → Boolean
    private final ConcurrentHashMap<String, Boolean> confirmMap = new ConcurrentHashMap<>();

    // sessionId → openid
    private final ConcurrentHashMap<String, String> sessionOpenid = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session, String openid) {
        sessionOpenid.put(session.getId(), openid);
    }

    /**
     * 尝试匹配。返回对方 session（匹配成功），或 empty（进入等待队列）。
     * 使用 compute 保证原子性。
     */
    public Optional<WebSocketSession> tryMatch(WebSocketSession session, Long spotId) {
        AtomicReference<WebSocketSession> partnerRef = new AtomicReference<>();
        waitingQueue.compute(spotId, (key, existing) -> {
            if (existing == null || existing.getId().equals(session.getId())) {
                return session; // 无人等待，入队
            }
            partnerRef.set(existing);
            return null; // 匹配成功，移出队列
        });

        WebSocketSession partner = partnerRef.get();
        if (partner != null) {
            matchedPairs.put(session.getId(), partner);
            matchedPairs.put(partner.getId(), session);
        }
        return Optional.ofNullable(partner);
    }

    public Optional<WebSocketSession> getPartner(String sessionId) {
        return Optional.ofNullable(matchedPairs.get(sessionId));
    }

    /**
     * 确认匹配。返回 true 表示双方均已确认。
     */
    public boolean confirm(String sessionId) {
        confirmMap.put(sessionId, true);
        WebSocketSession partner = matchedPairs.get(sessionId);
        return partner != null && Boolean.TRUE.equals(confirmMap.get(partner.getId()));
    }

    /** 取消匹配（等待中或已匹配均可调用）。 */
    public void cancel(WebSocketSession session) {
        String sessionId = session.getId();
        confirmMap.remove(sessionId);
        waitingQueue.values().remove(session);
        matchedPairs.remove(sessionId);
    }

    public void removeSession(WebSocketSession session) {
        cancel(session);
        sessionOpenid.remove(session.getId());
    }

    public String getOpenid(String sessionId) {
        return sessionOpenid.getOrDefault(sessionId, "");
    }
}
```

- [ ] **Step 4: 运行测试，确认全部通过**

```bash
./mvnw test -pl . -Dtest=MatchServiceTest -q 2>&1 | tail -10
```
Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/service/MatchService.java \
        src/test/java/com/LHZ/TripMate/service/MatchServiceTest.java
git commit -m "feat: 添加 MatchService 及单元测试"
```

---

## Task 6: 后端 — MatchWebSocketHandler

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/MatchWebSocketHandler.java`

- [ ] **Step 1: 创建文件**

```java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.dto.match.WsMessage;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MatchWebSocketHandler extends TextWebSocketHandler {

    private final MatchService matchService;
    private final ObjectMapper objectMapper;
    private final WxUserRepository wxUserRepository;

    // 所有活跃 sessions（sessionId → session）
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String openid = (String) session.getAttributes().get("openid");
        sessions.put(session.getId(), session);
        matchService.registerSession(session, openid);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessage msg = objectMapper.readValue(message.getPayload(), WsMessage.class);
        switch (msg.getType()) {
            case "join"     -> handleJoin(session, (Map<String, Object>) msg.getPayload());
            case "confirm"  -> handleConfirm(session);
            case "cancel"   -> handleCancel(session);
            case "location" -> handleLocation(session, (Map<String, Object>) msg.getPayload());
            case "leave"    -> handleLeave(session);
        }
    }

    private void handleJoin(WebSocketSession session, Map<String, Object> payload) throws Exception {
        Long spotId = Long.valueOf(payload.get("spotId").toString());
        String spotName = payload.getOrDefault("spotName", "未知景点").toString();

        Optional<WebSocketSession> partnerOpt = matchService.tryMatch(session, spotId);

        if (partnerOpt.isEmpty()) {
            send(session, WsMessage.of("waiting"));
        } else {
            WebSocketSession partner = partnerOpt.get();
            String myNickname = getNickname(matchService.getOpenid(session.getId()));
            String partnerNickname = getNickname(matchService.getOpenid(partner.getId()));

            send(session, WsMessage.of("matched",
                    Map.of("partnerNickname", partnerNickname, "spotName", spotName)));
            send(partner, WsMessage.of("matched",
                    Map.of("partnerNickname", myNickname, "spotName", spotName)));
        }
    }

    private void handleConfirm(WebSocketSession session) throws Exception {
        boolean bothConfirmed = matchService.confirm(session.getId());
        if (bothConfirmed) {
            send(session, WsMessage.of("confirmed"));
            matchService.getPartner(session.getId())
                    .ifPresent(p -> sendSilently(p, WsMessage.of("confirmed")));
        }
    }

    private void handleCancel(WebSocketSession session) throws Exception {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        matchService.cancel(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerCancelled")));
    }

    private void handleLocation(WebSocketSession session, Map<String, Object> payload) {
        matchService.getPartner(session.getId())
                .ifPresent(p -> sendSilently(p, WsMessage.of("locationUpdate", payload)));
    }

    private void handleLeave(WebSocketSession session) {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        matchService.cancel(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerLeft")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Optional<WebSocketSession> partnerOpt = matchService.getPartner(session.getId());
        sessions.remove(session.getId());
        matchService.removeSession(session);
        partnerOpt.ifPresent(p -> sendSilently(p, WsMessage.of("partnerLeft")));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private void send(WebSocketSession session, WsMessage msg) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void sendSilently(WebSocketSession session, WsMessage msg) {
        try { send(session, msg); } catch (Exception ignored) {}
    }

    private String getNickname(String openid) {
        return wxUserRepository.findByOpenid(openid)
                .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname() : "旅行者")
                .orElse("旅行者");
    }
}
```

- [ ] **Step 2: 整体编译验证（Task 4、5、6 联合）**

```bash
./mvnw compile -q
```
Expected: 无报错

- [ ] **Step 3: Commit Task 4 + Task 6**

```bash
git add src/main/java/com/LHZ/TripMate/config/WebSocketConfig.java \
        src/main/java/com/LHZ/TripMate/controller/MatchWebSocketHandler.java
git commit -m "feat: 添加 WebSocketConfig 和 MatchWebSocketHandler"
```

---

## Task 7: 后端 — SecurityConfig + ScenicSpotController 修改

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`
- Modify: `src/main/java/com/LHZ/TripMate/controller/ScenicSpotController.java`

- [ ] **Step 1: SecurityConfig — 加 /ws/** permitAll**

在 `authorizeHttpRequests` 中，在现有 `.requestMatchers("/api/admin/login"...)` 之前插入一行：

```java
.requestMatchers("/ws/**").permitAll()
```

完整 `authorizeHttpRequests` 块变为：
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/ws/**").permitAll()
    .requestMatchers("/api/admin/login", "/api/wx/login", "/api/badges").permitAll()
    .requestMatchers("/api/spots/**", "/api/weather/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)
```

- [ ] **Step 2: ScenicSpotController — findAll() 改返回 Result**

将：
```java
@GetMapping
public List<ScenicSpot> findAll() {
    return scenicSpotService.findAll();
}
```
改为：
```java
@GetMapping
public Result<List<ScenicSpot>> findAll() {
    return Result.success(scenicSpotService.findAll());
}
```

并在文件顶部加 import：
```java
import com.LHZ.TripMate.common.Result;
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -q
```
Expected: 无报错

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/config/SecurityConfig.java \
        src/main/java/com/LHZ/TripMate/controller/ScenicSpotController.java
git commit -m "feat: SecurityConfig 放行 /ws/**，ScenicSpotController 统一返回 Result"
```

---

## Task 8: 前端 — spot.ts

**Files:**
- Create: `frontend/api/spot.ts`

- [ ] **Step 1: 创建文件**

```typescript
import { useApi } from '@/utils/useApi'

export interface Spot {
  id: number
  name: string
  address: string
  region: string
  category: string
  latitude: number
  longitude: number
  imageUrl?: string
}

export function useSpotApi() {
  const { get } = useApi()

  function listSpots(): Promise<Spot[]> {
    return get<Spot[]>('/api/spots').then(r => r.data ?? [])
  }

  function searchSpots(keyword: string): Promise<Spot[]> {
    return get<Spot[]>('/api/spots/search', { keyword }).then(r => r.data ?? [])
  }

  return { listSpots, searchSpots }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/api/spot.ts
git commit -m "feat: 添加景区 API 封装 spot.ts"
```

---

## Task 9: 前端 — match.ts（WebSocket 管理器）

**Files:**
- Create: `frontend/api/match.ts`

- [ ] **Step 1: 创建文件**

```typescript
const WS_BASE = 'ws://localhost:8080/ws'

export interface WsMessage {
  type: string
  payload: Record<string, any>
}

export type MessageHandler = (msg: WsMessage) => void

let task: UniApp.SocketTask | null = null
let handler: MessageHandler | null = null

export function connectMatch(token: string, onMessage: MessageHandler): void {
  if (task) disconnectMatch()

  handler = onMessage
  task = uni.connectSocket({
    url: `${WS_BASE}?token=${token}`,
    complete: () => {}
  })

  task.onMessage((res) => {
    try {
      const msg: WsMessage = JSON.parse(res.data as string)
      handler?.(msg)
    } catch (e) {
      console.error('[match.ts] 解析消息失败', e)
    }
  })

  task.onError((err) => {
    console.error('[match.ts] WebSocket 错误', err)
  })
}

export function sendMatch(type: string, payload: Record<string, any> = {}): void {
  if (!task) return
  task.send({ data: JSON.stringify({ type, payload }) })
}

export function setMessageHandler(onMessage: MessageHandler): void {
  handler = onMessage
}

export function disconnectMatch(): void {
  if (!task) return
  try { task.close({}) } catch (_) {}
  task = null
  handler = null
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/api/match.ts
git commit -m "feat: 添加 WebSocket 管理器 match.ts"
```

---

## Task 10: 前端 — pages.json + locales

**Files:**
- Modify: `frontend/pages.json`
- Modify: `frontend/locales/zh.json`
- Modify: `frontend/locales/en.json`

- [ ] **Step 1: pages.json — 加入两个新页面**

在 `"pages"` 数组末尾（`pages/map/map` 之后）加入：

```json
{
  "path": "pages/match/match",
  "style": {
    "navigationBarTitleText": "开始旅游",
    "navigationBarBackgroundColor": "#ff6b35",
    "navigationBarTextStyle": "white"
  }
},
{
  "path": "pages/trip/trip",
  "style": {
    "navigationBarTitleText": "旅途中",
    "navigationBarBackgroundColor": "#1a1a2e",
    "navigationBarTextStyle": "white"
  }
}
```

- [ ] **Step 2: zh.json — 加 tabbar.match**

在 `"tabbar"` 对象里加一行：
```json
"match": "出发"
```

- [ ] **Step 3: en.json — 加 tabbar.match**

在 `"tabbar"` 对象里加一行：
```json
"match": "Go"
```

- [ ] **Step 4: Commit**

```bash
git add frontend/pages.json frontend/locales/zh.json frontend/locales/en.json
git commit -m "feat: 注册 match/trip 页面，添加 tabbar.match 翻译"
```

---

## Task 11: 前端 — TabBar.vue 改造

**Files:**
- Modify: `frontend/components/TabBar/TabBar.vue`

- [ ] **Step 1: 用以下内容完整替换 TabBar.vue**

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

interface Props {
  active?: string
}

const props = withDefaults(defineProps<Props>(), { active: 'home' })
const { t } = useI18n()

// 普通 tab（左2 + 右2）
const leftTabs  = [
  { key: 'home',  icon: '🏠', url: '/pages/index/index' },
  { key: 'guide', icon: '🗺️', url: '/pages/guide/guide' },
]
const rightTabs = [
  { key: 'language', icon: '🌐', url: '/pages/language/language' },
  { key: 'mine',     icon: '👤', url: '/pages/mine/mine' },
]

function switchTab(url: string, key: string) {
  if (key === props.active) return
  uni.redirectTo({ url })
}

function goMatch() {
  uni.navigateTo({ url: '/pages/match/match' })
}
</script>

<template>
  <view class="tabbar">
    <!-- 左侧 2 个 tab -->
    <view
      v-for="item in leftTabs"
      :key="item.key"
      class="tabbar-item"
      :class="{ active: item.key === props.active }"
      @click="switchTab(item.url, item.key)"
    >
      <view class="tabbar-icon">{{ item.icon }}</view>
      <view class="tabbar-text">{{ t(`tabbar.${item.key}`) }}</view>
    </view>

    <!-- 中间"出发"大按钮 -->
    <view class="tabbar-center" @click="goMatch">
      <view class="center-btn">
        <text class="center-icon">🧳</text>
      </view>
      <view class="tabbar-text center-text">{{ t('tabbar.match') }}</view>
    </view>

    <!-- 右侧 2 个 tab -->
    <view
      v-for="item in rightTabs"
      :key="item.key"
      class="tabbar-item"
      :class="{ active: item.key === props.active }"
      @click="switchTab(item.url, item.key)"
    >
      <view class="tabbar-icon">{{ item.icon }}</view>
      <view class="tabbar-text">{{ t(`tabbar.${item.key}`) }}</view>
    </view>
  </view>
</template>

<style scoped>
.tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  height: 120rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  display: flex;
  align-items: center;
  border-top: 1rpx solid #eee;
  z-index: 999;
}

.tabbar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #777;
}

.tabbar-item.active {
  color: #2f80ed;
  font-weight: 600;
}

.tabbar-icon { font-size: 42rpx; margin-bottom: 6rpx; }
.tabbar-text { font-size: 24rpx; }

/* 中间大按钮 */
.tabbar-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 10rpx;
  position: relative;
}

.center-btn {
  width: 108rpx;
  height: 108rpx;
  border-radius: 54rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 -6rpx 24rpx rgba(255, 107, 53, 0.45);
  position: absolute;
  top: -38rpx;
}

.center-icon { font-size: 48rpx; }
.center-text { color: #ff6b35; font-weight: 600; margin-top: 42rpx; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/components/TabBar/TabBar.vue
git commit -m "feat: TabBar 中间加"出发"大按钮"
```

---

## Task 12: 前端 — match.vue（匹配页）

**Files:**
- Create: `frontend/pages/match/match.vue`

- [ ] **Step 1: 创建文件**

```vue
<template>
  <view class="page">

    <!-- 阶段 1：选景点 -->
    <view v-if="step === 'select'" class="step-select">
      <view class="search-bar">
        <input
          class="search-input"
          placeholder="搜索景点…"
          v-model="keyword"
          @input="onSearch"
        />
      </view>

      <scroll-view class="spot-list" scroll-y>
        <view v-if="loading" class="hint">加载中…</view>
        <view v-else-if="spots.length === 0" class="hint">暂无景点数据</view>
        <view
          v-for="spot in spots"
          :key="spot.id"
          class="spot-item"
          :class="{ selected: selectedSpot?.id === spot.id }"
          @click="selectedSpot = spot"
        >
          <text class="spot-name">{{ spot.name }}</text>
          <text class="spot-region">{{ spot.region || spot.address }}</text>
        </view>
      </scroll-view>

      <view class="bottom-bar">
        <button
          class="start-btn"
          :disabled="!selectedSpot"
          @click="startMatch"
        >
          {{ selectedSpot ? `前往 ${selectedSpot.name}，开始匹配` : '请先选择景点' }}
        </button>
      </view>
    </view>

    <!-- 阶段 2：等待中 -->
    <view v-else-if="step === 'waiting'" class="step-waiting">
      <view class="spin-wrap">
        <view class="spin-ring" />
        <text class="spin-icon">🧳</text>
      </view>
      <text class="waiting-spot">{{ selectedSpot?.name }}</text>
      <text class="waiting-tip">正在寻找搭子…</text>
      <button class="cancel-btn" @click="cancelMatch">取消匹配</button>
    </view>

    <!-- 阶段 3：匹配成功确认 -->
    <view v-else-if="step === 'matched'" class="step-matched">
      <view class="matched-card">
        <text class="matched-title">🎉 发现搭子！</text>
        <text class="matched-partner">{{ partnerNickname }}</text>
        <text class="matched-spot">目的地：{{ selectedSpot?.name }}</text>
        <text class="countdown-tip">{{ countdown }} 秒后自动取消</text>
        <view class="matched-actions">
          <button class="confirm-btn" @click="confirmMatch">确认出发</button>
          <button class="cancel-btn-sm" @click="cancelMatch">取消</button>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useSpotApi, type Spot } from '@/api/spot'
import { connectMatch, sendMatch, disconnectMatch } from '@/api/match'

const { listSpots, searchSpots } = useSpotApi()

type Step = 'select' | 'waiting' | 'matched'
const step = ref<Step>('select')
// 跳转旅途页时设为 true，阻止 onUnmounted 关闭 WS 连接
let transitioning = false

const spots = ref<Spot[]>([])
const loading = ref(true)
const keyword = ref('')
const selectedSpot = ref<Spot | null>(null)
const partnerNickname = ref('')
const countdown = ref(15)

let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  try {
    spots.value = await listSpots()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  // transitioning 为 true 表示正跳转旅途页，保留 WS 连接给 trip.vue 复用
  if (!transitioning) disconnectMatch()
  clearCountdown()
})

async function onSearch() {
  loading.value = true
  try {
    spots.value = await searchSpots(keyword.value)
  } finally {
    loading.value = false
  }
}

function startMatch() {
  if (!selectedSpot.value) return
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  step.value = 'waiting'
  connectMatch(token, onWsMessage)

  // 等连接建立后发送 join（给一点延迟保证握手完成）
  setTimeout(() => {
    uni.getLocation({
      type: 'gcj02',
      success: (loc) => {
        sendMatch('join', {
          spotId: selectedSpot.value!.id,
          spotName: selectedSpot.value!.name,
          latitude: loc.latitude,
          longitude: loc.longitude,
        })
      },
      fail: () => {
        sendMatch('join', {
          spotId: selectedSpot.value!.id,
          spotName: selectedSpot.value!.name,
          latitude: 0,
          longitude: 0,
        })
      }
    })
  }, 500)
}

function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  if (msg.type === 'waiting') {
    step.value = 'waiting'
  } else if (msg.type === 'matched') {
    partnerNickname.value = msg.payload.partnerNickname ?? '旅行者'
    step.value = 'matched'
    startCountdown()
  } else if (msg.type === 'confirmed') {
    clearCountdown()
    transitioning = true // trip.vue 会复用此 WS 连接，不能关闭
    uni.redirectTo({
      url: `/pages/trip/trip?spotId=${selectedSpot.value!.id}&spotName=${encodeURIComponent(selectedSpot.value!.name)}&partnerNickname=${encodeURIComponent(partnerNickname.value)}`
    })
  } else if (msg.type === 'partnerCancelled') {
    clearCountdown()
    step.value = 'select'
    uni.showToast({ title: '搭子取消了，重新匹配', icon: 'none' })
  }
}

function confirmMatch() {
  sendMatch('confirm')
}

function cancelMatch() {
  clearCountdown()
  sendMatch('cancel')
  disconnectMatch()
  step.value = 'select'
}

function startCountdown() {
  countdown.value = 15
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
      cancelMatch()
      uni.showToast({ title: '确认超时，已自动取消', icon: 'none' })
    }
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f6f7; display: flex; flex-direction: column; }

/* 选景点 */
.step-select { flex: 1; display: flex; flex-direction: column; }
.search-bar { padding: 20rpx 24rpx; background: #fff; border-bottom: 1rpx solid #eee; }
.search-input {
  width: 100%; height: 72rpx; background: #f0f2f5;
  border-radius: 36rpx; padding: 0 28rpx;
  font-size: 28rpx; box-sizing: border-box;
}
.spot-list { flex: 1; }
.hint { text-align: center; color: #aaa; font-size: 28rpx; padding: 60rpx 0; }
.spot-item {
  background: #fff; margin: 16rpx 24rpx; padding: 28rpx 32rpx;
  border-radius: 16rpx; border: 2rpx solid transparent;
}
.spot-item.selected { border-color: #ff6b35; background: #fff8f5; }
.spot-name { font-size: 30rpx; font-weight: 600; color: #222; display: block; }
.spot-region { font-size: 24rpx; color: #888; margin-top: 6rpx; display: block; }
.bottom-bar { padding: 24rpx; background: #fff; border-top: 1rpx solid #eee; }
.start-btn {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff; border-radius: 44rpx; font-size: 32rpx; font-weight: 600; border: none;
}
.start-btn[disabled] { background: #ccc; }

/* 等待中 */
.step-waiting {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 32rpx;
}
.spin-wrap {
  width: 200rpx; height: 200rpx; position: relative;
  display: flex; align-items: center; justify-content: center;
}
.spin-ring {
  position: absolute; inset: 0;
  border: 8rpx solid #ff6b35; border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
.spin-icon { font-size: 80rpx; }
@keyframes spin { to { transform: rotate(360deg); } }
.waiting-spot { font-size: 36rpx; font-weight: 700; color: #222; }
.waiting-tip { font-size: 28rpx; color: #888; }
.cancel-btn {
  margin-top: 40rpx; padding: 20rpx 80rpx;
  background: #f5f5f5; color: #666; border-radius: 44rpx;
  font-size: 28rpx; border: none;
}

/* 匹配成功 */
.step-matched {
  flex: 1; display: flex; align-items: center; justify-content: center; padding: 40rpx;
}
.matched-card {
  background: #fff; border-radius: 32rpx; padding: 60rpx 48rpx;
  width: 100%; display: flex; flex-direction: column; align-items: center;
  gap: 20rpx; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.1);
}
.matched-title { font-size: 40rpx; font-weight: 700; }
.matched-partner { font-size: 52rpx; font-weight: 700; color: #ff6b35; }
.matched-spot { font-size: 28rpx; color: #666; }
.countdown-tip { font-size: 24rpx; color: #aaa; }
.matched-actions { display: flex; gap: 24rpx; margin-top: 20rpx; width: 100%; }
.confirm-btn {
  flex: 2; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff; border-radius: 44rpx; font-size: 30rpx; font-weight: 600; border: none;
}
.cancel-btn-sm {
  flex: 1; height: 88rpx; line-height: 88rpx;
  background: #f5f5f5; color: #666; border-radius: 44rpx; font-size: 28rpx; border: none;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/pages/match/match.vue
git commit -m "feat: 添加匹配页 match.vue"
```

---

## Task 13: 前端 — trip.vue（旅途页）

**Files:**
- Create: `frontend/pages/trip/trip.vue`

- [ ] **Step 1: 创建文件**

```vue
<template>
  <view class="page">
    <!-- 地图 -->
    <map
      class="map"
      :latitude="myLat"
      :longitude="myLng"
      :scale="14"
      :markers="markers"
      :show-location="false"
    />

    <!-- 底部面板 -->
    <view class="panel">
      <view class="panel-row">
        <text class="panel-label">目的地</text>
        <text class="panel-value">{{ spotName }}</text>
      </view>
      <view class="panel-row">
        <text class="panel-label">搭子</text>
        <text class="panel-value">{{ partnerNickname }}</text>
      </view>
      <view class="panel-row">
        <text class="panel-label">与搭子距离</text>
        <text class="panel-value">{{ distanceText }}</text>
      </view>
      <button class="leave-btn" @click="leaveTrip">结束旅途</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { setMessageHandler, sendMatch, disconnectMatch } from '@/api/match'

const spotName = ref('')
const partnerNickname = ref('')
const spotId = ref(0)

const myLat = ref(29.8266)
const myLng = ref(106.422)
const partnerLat = ref<number | null>(null)
const partnerLng = ref<number | null>(null)

let locationTimer: ReturnType<typeof setInterval> | null = null

const markers = computed(() => {
  const list: any[] = [
    {
      id: 1,
      latitude: myLat.value,
      longitude: myLng.value,
      title: '我',
      width: 40, height: 40,
      iconPath: '', // 使用默认蓝色标记
      callout: { content: '我', color: '#fff', bgColor: '#2196f3', padding: 8, borderRadius: 8, display: 'ALWAYS' }
    }
  ]
  if (partnerLat.value !== null && partnerLng.value !== null) {
    list.push({
      id: 2,
      latitude: partnerLat.value,
      longitude: partnerLng.value,
      title: partnerNickname.value,
      width: 40, height: 40,
      iconPath: '',
      callout: { content: partnerNickname.value, color: '#fff', bgColor: '#f44336', padding: 8, borderRadius: 8, display: 'ALWAYS' }
    })
  }
  return list
})

const distanceText = computed(() => {
  if (partnerLat.value === null || partnerLng.value === null) return '等待搭子位置…'
  const d = haversine(myLat.value, myLng.value, partnerLat.value, partnerLng.value)
  return d < 1000 ? `${Math.round(d)} 米` : `${(d / 1000).toFixed(1)} 千米`
})

onLoad((query) => {
  spotId.value = Number(query?.spotId ?? 0)
  spotName.value = decodeURIComponent(query?.spotName ?? '')
  partnerNickname.value = decodeURIComponent(query?.partnerNickname ?? '搭子')
})

onMounted(() => {
  // 复用 match.vue 已建立的 WS 连接，只替换消息处理器
  setMessageHandler(onWsMessage)
  updateMyLocation()
  locationTimer = setInterval(updateMyLocation, 3000)
})

onUnmounted(() => {
  if (locationTimer) clearInterval(locationTimer)
  disconnectMatch()
})

function onWsMessage(msg: { type: string; payload: Record<string, any> }) {
  if (msg.type === 'locationUpdate') {
    partnerLat.value = msg.payload.latitude
    partnerLng.value = msg.payload.longitude
  } else if (msg.type === 'partnerLeft') {
    uni.showModal({
      title: '搭子已离开',
      content: '搭子结束了旅途',
      showCancel: false,
    })
    partnerLat.value = null
    partnerLng.value = null
  }
}

function updateMyLocation() {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      myLat.value = res.latitude
      myLng.value = res.longitude
      sendMatch('location', { latitude: res.latitude, longitude: res.longitude })
    }
  })
}

function leaveTrip() {
  uni.showModal({
    title: '结束旅途',
    content: '确定要结束本次旅途吗？',
    success: (res) => {
      if (res.confirm) {
        sendMatch('leave')
        disconnectMatch()
        uni.redirectTo({ url: '/pages/index/index' })
      }
    }
  })
}

/** Haversine 公式，返回米 */
function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; }
.map { width: 100%; flex: 1; }

.panel {
  background: #fff;
  padding: 28rpx 32rpx;
  border-top: 1rpx solid #eee;
}
.panel-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.panel-label { font-size: 26rpx; color: #888; }
.panel-value { font-size: 28rpx; color: #222; font-weight: 500; }

.leave-btn {
  margin-top: 24rpx; width: 100%; height: 88rpx; line-height: 88rpx;
  background: #1a1a2e; color: #fff;
  border-radius: 44rpx; font-size: 30rpx; border: none;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/pages/trip/trip.vue
git commit -m "feat: 添加旅途页 trip.vue"
```

---

## Task 14: 整体联调验证

- [ ] **Step 1: 重启后端**

```bash
./mvnw spring-boot:run
```
确认日志出现：`Mapped URL path [/ws] onto handler of type [class com.LHZ.TripMate.controller.MatchWebSocketHandler]`

- [ ] **Step 2: HBuilderX 重新编译前端**

在 HBuilderX 点击"运行 → 运行到小程序模拟器 → 微信开发者工具"，等待编译完成。

- [ ] **Step 3: 验证 TabBar 大按钮**

打开模拟器，确认 TabBar 中间出现橙色圆形"出发"按钮，点击能跳转到匹配页。

- [ ] **Step 4: 验证景点列表**

匹配页出现景点列表，能搜索过滤，选中后"开始匹配"按钮变为激活状态。

- [ ] **Step 5: 验证双端匹配（用两个已登录 session 测试）**

打开两个微信开发者工具窗口（或用 curl 测试 WebSocket），两个用户选同一景点点"开始匹配"，确认双方都收到"发现搭子！"弹窗。

- [ ] **Step 6: 验证确认流程**

两端都点"确认出发"，确认跳转到旅途页，地图上出现两个位置标记。

- [ ] **Step 7: Final commit**

```bash
git add -A
git commit -m "feat: 完成开始旅游实时匹配功能"
```
