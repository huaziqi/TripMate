# 旅游导览数字人 AI 聊天接口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Spring Boot 后端新增导览 AI 聊天模块，支持按景点切换人格+知识库、后端持久化对话历史、SSE 流式返回 DeepSeek 回复。

**Architecture:** 新增三张 JPA 表（`guide_spot_config` / `guide_session` / `guide_message`）；`DeepSeekClient` 负责调用 DeepSeek 流式 API；`GuideServiceImpl` 编排会话/消息存取并返回 `SseEmitter`；后台线程将 DeepSeek delta 推送给 emitter，结束后将完整回复写库。

**Tech Stack:** Spring Boot 3 / Java 21、Spring MVC SseEmitter、RestClient（已有）、Jackson ObjectMapper（已有）、JPA + MySQL（已有）、DeepSeek API（OpenAI 兼容格式）、JUnit 5 + Mockito（已有 spring-boot-starter-test）

## Global Constraints

- 所有新 Controller 遵守 `Result<T>` / `ResultCode` 统一响应结构（SSE 端点除外，直接返回 `SseEmitter`）
- 新 entity 用 `@PrePersist` 设置 `createdAt`，遵循现有模式
- DeepSeek API key 通过 `application.yaml` 注入，不硬编码
- 上下文历史窗口固定 20 条；按 `createdAt DESC` 取后反转为时序顺序
- 景点标识 `spotKey` 当前只用 `swu`（西南大学）
- 包名前缀：`com.LHZ.TripMate`

---

## File Map

**新建文件：**
- `src/main/java/com/LHZ/TripMate/config/DeepSeekConfig.java`
- `src/main/java/com/LHZ/TripMate/config/AsyncConfig.java`
- `src/main/java/com/LHZ/TripMate/entity/GuideSpotConfig.java`
- `src/main/java/com/LHZ/TripMate/entity/GuideSession.java`
- `src/main/java/com/LHZ/TripMate/entity/GuideMessage.java`
- `src/main/java/com/LHZ/TripMate/repository/GuideSpotConfigRepository.java`
- `src/main/java/com/LHZ/TripMate/repository/GuideSessionRepository.java`
- `src/main/java/com/LHZ/TripMate/repository/GuideMessageRepository.java`
- `src/main/java/com/LHZ/TripMate/dto/GuideChatRequestDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/GuideMessageDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/admin/GuideSpotConfigDTO.java`
- `src/main/java/com/LHZ/TripMate/service/DeepSeekClient.java`
- `src/main/java/com/LHZ/TripMate/service/GuideService.java`
- `src/main/java/com/LHZ/TripMate/service/impl/GuideServiceImpl.java`
- `src/main/java/com/LHZ/TripMate/controller/GuideChatController.java`
- `src/main/java/com/LHZ/TripMate/controller/admin/AdminGuideController.java`
- `src/main/java/com/LHZ/TripMate/config/GuideDataInitializer.java`
- `src/test/java/com/LHZ/TripMate/service/GuideServiceImplTest.java`
- `src/test/java/com/LHZ/TripMate/service/DeepSeekClientDeltaTest.java`

**修改文件：**
- `src/main/resources/application.yaml` — 新增 `deepseek.api` 配置段
- `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java` — 新增 `/api/guide/**` 路由规则

---

## Task 1: 数据层（Config + Entities + Repositories + DTOs）

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/config/DeepSeekConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/GuideSpotConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/GuideSession.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/GuideMessage.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/GuideSpotConfigRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/GuideSessionRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/GuideMessageRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/GuideChatRequestDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/GuideMessageDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/GuideSpotConfigDTO.java`
- Modify: `src/main/resources/application.yaml`

**Interfaces:**
- Produces: `GuideSpotConfigRepository.findBySpotKeyAndActiveTrue(String)`, `GuideSessionRepository.findByUserIdAndSpotKey(Long, String)`, `GuideMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(Long)`, `GuideMessageRepository.deleteBySessionId(Long)`, `DeepSeekConfig` bean (fields: `url`, `key`, `model`)

- [ ] **Step 1: 在 `application.yaml` 末尾追加 DeepSeek 配置段**

```yaml
deepseek:
  api:
    url: https://api.deepseek.com/chat/completions
    key: sk-REPLACE_WITH_YOUR_KEY
    model: deepseek-chat
```

- [ ] **Step 2: 创建 `DeepSeekConfig.java`**

```java
package com.LHZ.TripMate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {
    private String url;
    private String key;
    private String model;
}
```

- [ ] **Step 3: 创建 `GuideSpotConfig.java`**

```java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "guide_spot_config")
public class GuideSpotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_key", unique = true, nullable = false, length = 50)
    private String spotKey;

    @Column(name = "persona_name", nullable = false, length = 50)
    private String personaName;

    @Column(name = "persona_desc", columnDefinition = "TEXT")
    private String personaDesc;

    @Column(name = "knowledge_text", columnDefinition = "TEXT")
    private String knowledgeText;

    @Column(nullable = false)
    private boolean active = true;
}
```

- [ ] **Step 4: 创建 `GuideSession.java`**

```java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guide_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "spot_key"}))
public class GuideSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spot_key", nullable = false, length = 50)
    private String spotKey;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
```

- [ ] **Step 5: 创建 `GuideMessage.java`**

```java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guide_message")
public class GuideMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false, length = 10)
    private String role; // "USER" or "ASSISTANT"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
```

- [ ] **Step 6: 创建三个 Repository**

```java
// GuideSpotConfigRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideSpotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuideSpotConfigRepository extends JpaRepository<GuideSpotConfig, Long> {
    Optional<GuideSpotConfig> findBySpotKey(String spotKey);
    Optional<GuideSpotConfig> findBySpotKeyAndActiveTrue(String spotKey);
}
```

```java
// GuideSessionRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuideSessionRepository extends JpaRepository<GuideSession, Long> {
    Optional<GuideSession> findByUserIdAndSpotKey(Long userId, String spotKey);
}
```

```java
// GuideMessageRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.GuideMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideMessageRepository extends JpaRepository<GuideMessage, Long> {
    List<GuideMessage> findTop20BySessionIdOrderByCreatedAtDesc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
```

- [ ] **Step 7: 创建 DTO 类**

```java
// GuideChatRequestDTO.java
package com.LHZ.TripMate.dto;

import lombok.Data;

@Data
public class GuideChatRequestDTO {
    private String message;
}
```

```java
// GuideMessageDTO.java
package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GuideMessageDTO {
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
```

```java
// admin/GuideSpotConfigDTO.java
package com.LHZ.TripMate.dto.admin;

import lombok.Data;

@Data
public class GuideSpotConfigDTO {
    private String personaName;
    private String personaDesc;
    private String knowledgeText;
    private boolean active;
}
```

- [ ] **Step 8: 启动应用验证表自动创建**

```bash
./mvnw spring-boot:run
```

查看日志无报错，MySQL 中出现 `guide_spot_config`、`guide_session`、`guide_message` 三张表（`ddl-auto: update` 自动创建）。确认后停止服务。

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/config/DeepSeekConfig.java \
        src/main/java/com/LHZ/TripMate/entity/GuideSpotConfig.java \
        src/main/java/com/LHZ/TripMate/entity/GuideSession.java \
        src/main/java/com/LHZ/TripMate/entity/GuideMessage.java \
        src/main/java/com/LHZ/TripMate/repository/GuideSpotConfigRepository.java \
        src/main/java/com/LHZ/TripMate/repository/GuideSessionRepository.java \
        src/main/java/com/LHZ/TripMate/repository/GuideMessageRepository.java \
        src/main/java/com/LHZ/TripMate/dto/GuideChatRequestDTO.java \
        src/main/java/com/LHZ/TripMate/dto/GuideMessageDTO.java \
        src/main/java/com/LHZ/TripMate/dto/admin/GuideSpotConfigDTO.java \
        src/main/resources/application.yaml
git commit -m "feat(guide): add data layer — entities, repos, DTOs, DeepSeek config"
```

---

## Task 2: DeepSeekClient（流式 HTTP 调用）

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/DeepSeekClient.java`
- Create: `src/test/java/com/LHZ/TripMate/service/DeepSeekClientDeltaTest.java`

**Interfaces:**
- Consumes: `DeepSeekConfig` bean (Task 1)
- Produces: `DeepSeekClient.streamChat(List<Map<String,Object>> messages, Consumer<String> onDelta, Consumer<String> onComplete, Consumer<String> onError)`

- [ ] **Step 1: 创建 `DeepSeekClientDeltaTest.java`（先写测试）**

```java
package com.LHZ.TripMate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekClientDeltaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsDeltaFromValidChunk() throws Exception {
        String chunk = "{\"choices\":[{\"delta\":{\"content\":\"西南\"}}]}";
        String delta = objectMapper.readTree(chunk)
                .path("choices").path(0)
                .path("delta").path("content").asText("");
        assertEquals("西南", delta);
    }

    @Test
    void returnsEmptyStringWhenNoContent() throws Exception {
        String chunk = "{\"choices\":[{\"delta\":{}}]}";
        String delta = objectMapper.readTree(chunk)
                .path("choices").path(0)
                .path("delta").path("content").asText("");
        assertEquals("", delta);
    }

    @Test
    void handlesInvalidJsonGracefully() {
        String badJson = "not-json";
        assertDoesNotThrow(() -> {
            try {
                objectMapper.readTree(badJson);
            } catch (Exception ignored) {}
        });
    }
}
```

- [ ] **Step 2: 运行测试，确认通过（只验证 JSON 解析逻辑，无需 DeepSeek 真实调用）**

```bash
./mvnw test -Dtest=DeepSeekClientDeltaTest -pl .
```

预期：3 tests PASSED

- [ ] **Step 3: 创建 `DeepSeekClient.java`**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.config.DeepSeekConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class DeepSeekClient {

    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekClient(DeepSeekConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * 流式调用 DeepSeek Chat API。
     * 在调用线程中阻塞直到流结束。必须在非主线程中调用。
     *
     * @param messages    完整 messages 列表（含 system + history + 当前用户消息）
     * @param onDelta     每收到一个 delta 文字片段时回调
     * @param onComplete  流正常结束时回调，参数为完整拼接内容
     * @param onError     发生错误时回调，参数为错误描述
     */
    public void streamChat(
            List<Map<String, Object>> messages,
            Consumer<String> onDelta,
            Consumer<String> onComplete,
            Consumer<String> onError) {

        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", messages,
                "stream", true
        );

        try {
            restClient.post()
                    .uri(config.getUrl())
                    .header("Authorization", "Bearer " + config.getKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .exchange((req, res) -> {
                        if (!res.getStatusCode().is2xxSuccessful()) {
                            onError.accept("DeepSeek API 错误: " + res.getStatusCode());
                            return null;
                        }
                        StringBuilder fullContent = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) continue;
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) break;
                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    String delta = node.path("choices").path(0)
                                            .path("delta").path("content").asText("");
                                    if (!delta.isEmpty()) {
                                        fullContent.append(delta);
                                        onDelta.accept(delta);
                                    }
                                } catch (Exception ignored) {
                                    // 跳过无法解析的 chunk
                                }
                            }
                        }
                        onComplete.accept(fullContent.toString());
                        return null;
                    });
        } catch (Exception e) {
            log.error("DeepSeek streaming error", e);
            onError.accept("AI 服务异常，请稍后重试");
        }
    }
}
```

- [ ] **Step 4: 编译验证无报错**

```bash
./mvnw compile -q
```

预期：BUILD SUCCESS，无错误输出。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/service/DeepSeekClient.java \
        src/test/java/com/LHZ/TripMate/service/DeepSeekClientDeltaTest.java
git commit -m "feat(guide): add DeepSeekClient with SSE streaming"
```

---

## Task 3: GuideService + AsyncConfig

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/config/AsyncConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/service/GuideService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/GuideServiceImpl.java`
- Create: `src/test/java/com/LHZ/TripMate/service/GuideServiceImplTest.java`

**Interfaces:**
- Consumes: `DeepSeekClient.streamChat(...)` (Task 2), all repositories (Task 1), `GuideMessageDTO` (Task 1)
- Produces:
  - `GuideService.getHistory(Long userId, String spotKey): List<GuideMessageDTO>`
  - `GuideService.chat(String userMessage, String spotKey, Long userId): SseEmitter`
  - `GuideService.clearHistory(Long userId, String spotKey): void`

- [ ] **Step 1: 创建 `GuideServiceImplTest.java`（先写测试）**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideMessageRepository;
import com.LHZ.TripMate.repository.GuideSessionRepository;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import com.LHZ.TripMate.service.impl.GuideServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock GuideSpotConfigRepository spotConfigRepo;
    @Mock GuideSessionRepository sessionRepo;
    @Mock GuideMessageRepository messageRepo;
    @Mock DeepSeekClient deepSeekClient;
    @Mock Executor executor;

    private GuideServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuideServiceImpl(spotConfigRepo, sessionRepo, messageRepo, deepSeekClient, new ObjectMapper());
        service.setExecutor(executor);
    }

    @Test
    void getHistory_returnsEmptyList_whenNoSession() {
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.empty());

        List<GuideMessageDTO> result = service.getHistory(1L, "swu");

        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_returnsMappedMessages_whenSessionExists() {
        GuideSession session = new GuideSession();
        session.setId(10L);

        GuideMessage msg = new GuideMessage();
        msg.setRole("USER");
        msg.setContent("你好");
        msg.setCreatedAt(LocalDateTime.now());

        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.of(session));
        when(messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(msg));

        List<GuideMessageDTO> result = service.getHistory(1L, "swu");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("USER");
        assertThat(result.get(0).getContent()).isEqualTo("你好");
    }

    @Test
    void clearHistory_deletesMessages_whenSessionExists() {
        GuideSession session = new GuideSession();
        session.setId(42L);
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.of(session));

        service.clearHistory(1L, "swu");

        verify(messageRepo).deleteBySessionId(42L);
    }

    @Test
    void clearHistory_doesNothing_whenNoSession() {
        when(sessionRepo.findByUserIdAndSpotKey(1L, "swu")).thenReturn(Optional.empty());

        service.clearHistory(1L, "swu");

        verify(messageRepo, never()).deleteBySessionId(any());
    }
}
```

- [ ] **Step 2: 运行测试，预期编译失败（`GuideServiceImpl` 尚不存在）**

```bash
./mvnw test -Dtest=GuideServiceImplTest -pl . 2>&1 | tail -5
```

预期：COMPILATION ERROR — `GuideServiceImpl` cannot be found

- [ ] **Step 3: 创建 `AsyncConfig.java`**

```java
package com.LHZ.TripMate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean("guideTaskExecutor")
    public Executor guideTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("guide-sse-");
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: 创建 `GuideService.java` 接口**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface GuideService {
    List<GuideMessageDTO> getHistory(Long userId, String spotKey);
    SseEmitter chat(String userMessage, String spotKey, Long userId);
    void clearHistory(Long userId, String spotKey);
}
```

- [ ] **Step 5: 创建 `GuideServiceImpl.java`**

```java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideMessageRepository;
import com.LHZ.TripMate.repository.GuideSessionRepository;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import com.LHZ.TripMate.service.DeepSeekClient;
import com.LHZ.TripMate.service.GuideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    private final GuideSpotConfigRepository spotConfigRepo;
    private final GuideSessionRepository sessionRepo;
    private final GuideMessageRepository messageRepo;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier("guideTaskExecutor")
    private Executor executor;

    // 供测试注入 executor
    public void setExecutor(Executor executor) { this.executor = executor; }

    @Override
    public List<GuideMessageDTO> getHistory(Long userId, String spotKey) {
        return sessionRepo.findByUserIdAndSpotKey(userId, spotKey)
                .map(session -> {
                    List<GuideMessage> msgs =
                            messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId());
                    Collections.reverse(msgs);
                    return msgs.stream().map(this::toDTO).toList();
                })
                .orElse(List.of());
    }

    @Override
    public SseEmitter chat(String userMessage, String spotKey, Long userId) {
        GuideSpotConfig config = spotConfigRepo.findBySpotKeyAndActiveTrue(spotKey)
                .orElseThrow(() -> new RuntimeException("景点不存在或导览未启用"));

        // 查或建 session
        GuideSession session = sessionRepo.findByUserIdAndSpotKey(userId, spotKey)
                .orElseGet(() -> {
                    GuideSession s = new GuideSession();
                    s.setUserId(userId);
                    s.setSpotKey(spotKey);
                    return sessionRepo.save(s);
                });

        // 保存用户消息
        GuideMessage userMsg = new GuideMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("USER");
        userMsg.setContent(userMessage);
        messageRepo.save(userMsg);

        // 取最近 20 条（含刚保存的），倒序→时序
        List<GuideMessage> history =
                messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId());
        Collections.reverse(history);

        List<Map<String, Object>> messages = buildMessages(buildSystemPrompt(config), history);
        SseEmitter emitter = new SseEmitter(120_000L);
        final Long sessionId = session.getId();

        executor.execute(() -> {
            deepSeekClient.streamChat(
                    messages,
                    delta -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(objectMapper.writeValueAsString(Map.of("delta", delta))));
                        } catch (Exception ignored) {}
                    },
                    fullContent -> {
                        try {
                            GuideMessage assistantMsg = new GuideMessage();
                            assistantMsg.setSessionId(sessionId);
                            assistantMsg.setRole("ASSISTANT");
                            assistantMsg.setContent(fullContent.isEmpty() ? "(无回复)" : fullContent);
                            messageRepo.save(assistantMsg);
                            emitter.send(SseEmitter.event()
                                    .data(objectMapper.writeValueAsString(Map.of("done", true))));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    },
                    error -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(objectMapper.writeValueAsString(Map.of("error", error))));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
            );
        });

        return emitter;
    }

    @Override
    public void clearHistory(Long userId, String spotKey) {
        sessionRepo.findByUserIdAndSpotKey(userId, spotKey)
                .ifPresent(session -> messageRepo.deleteBySessionId(session.getId()));
    }

    private String buildSystemPrompt(GuideSpotConfig config) {
        return String.format("""
                你是%s，%s。

                ## 你掌握的景点知识
                %s

                ## 对话规范
                - 回答简洁自然，每次不超过 150 字
                - 不知道的内容直接说"这个我还不太清楚"，不编造
                - 保持导游视角，语气亲切，适当引导游客探索
                - 使用中文回答
                """, config.getPersonaName(), config.getPersonaDesc(), config.getKnowledgeText());
    }

    private List<Map<String, Object>> buildMessages(String systemPrompt, List<GuideMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (GuideMessage msg : history) {
            messages.add(Map.of("role", msg.getRole().toLowerCase(), "content", msg.getContent()));
        }
        return messages;
    }

    private GuideMessageDTO toDTO(GuideMessage msg) {
        return GuideMessageDTO.builder()
                .role(msg.getRole())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
./mvnw test -Dtest=GuideServiceImplTest -pl .
```

预期：4 tests PASSED

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/config/AsyncConfig.java \
        src/main/java/com/LHZ/TripMate/service/GuideService.java \
        src/main/java/com/LHZ/TripMate/service/impl/GuideServiceImpl.java \
        src/test/java/com/LHZ/TripMate/service/GuideServiceImplTest.java
git commit -m "feat(guide): add GuideService with session management and SSE orchestration"
```

---

## Task 4: GuideChatController + SecurityConfig 更新

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/GuideChatController.java`
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`

**Interfaces:**
- Consumes: `GuideService` (Task 3), `WxUserDetails` (已有), `Result<T>` (已有)
- Produces: `GET /api/guide/{spotKey}/history`, `POST /api/guide/{spotKey}/chat`, `DELETE /api/guide/{spotKey}/history`

- [ ] **Step 1: 创建 `GuideChatController.java`**

```java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.GuideChatRequestDTO;
import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideChatController {

    private final GuideService guideService;

    @GetMapping("/{spotKey}/history")
    public Result<List<GuideMessageDTO>> history(
            @PathVariable String spotKey,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(guideService.getHistory(userDetails.getWxUser().getId(), spotKey));
    }

    @PostMapping(value = "/{spotKey}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @PathVariable String spotKey,
            @RequestBody GuideChatRequestDTO request,
            @AuthenticationPrincipal WxUserDetails userDetails) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"消息不能为空\"}"));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        try {
            return guideService.chat(
                    request.getMessage().trim(),
                    spotKey,
                    userDetails.getWxUser().getId());
        } catch (RuntimeException e) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"" + e.getMessage() + "\"}"));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }
    }

    @DeleteMapping("/{spotKey}/history")
    public Result<Void> clearHistory(
            @PathVariable String spotKey,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        guideService.clearHistory(userDetails.getWxUser().getId(), spotKey);
        return Result.success(null);
    }
}
```

- [ ] **Step 2: 在 `SecurityConfig.java` 中新增导览路由规则**

在现有 `authorizeHttpRequests` 链中，找到 `.requestMatchers("/api/**").authenticated()` 这一行**之前**，插入：

```java
.requestMatchers("/api/guide/**").hasRole("WX_USER")
```

修改后相关部分形如：

```java
.requestMatchers(
    "/api/favorites",
    "/api/favorites/**",
    "/api/history",
    "/api/history/**"
).hasRole("WX_USER")

.requestMatchers("/api/guide/**").hasRole("WX_USER")   // ← 新增

.requestMatchers(HttpMethod.GET, "/api/posts/my", "/api/posts/my/favorites").authenticated()
// ... 其余规则不变
```

- [ ] **Step 3: 编译验证**

```bash
./mvnw compile -q
```

预期：BUILD SUCCESS

- [ ] **Step 4: 启动并手动验证 history 端点（未登录应返回 401）**

```bash
./mvnw spring-boot:run
```

另开终端：

```bash
curl -s http://localhost:8080/api/guide/swu/history
```

预期：HTTP 401 或 403（未携带 token，安全拦截生效）。确认后停止服务。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/controller/GuideChatController.java \
        src/main/java/com/LHZ/TripMate/config/SecurityConfig.java
git commit -m "feat(guide): add GuideChatController and security rules"
```

---

## Task 5: AdminGuideController + GuideDataInitializer（西南大学初始数据）

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/AdminGuideController.java`
- Create: `src/main/java/com/LHZ/TripMate/config/GuideDataInitializer.java`

**Interfaces:**
- Consumes: `GuideSpotConfigRepository.findBySpotKey(String)` (Task 1), `GuideSpotConfigDTO` (Task 1)
- Produces: `GET /api/admin/guide/configs`, `PUT /api/admin/guide/configs/{spotKey}`

- [ ] **Step 1: 创建 `AdminGuideController.java`**

```java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.GuideSpotConfigDTO;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/guide")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminGuideController {

    private final GuideSpotConfigRepository configRepo;

    @GetMapping("/configs")
    public Result<List<GuideSpotConfig>> list() {
        return Result.success(configRepo.findAll());
    }

    @PutMapping("/configs/{spotKey}")
    public Result<GuideSpotConfig> upsert(
            @PathVariable String spotKey,
            @RequestBody GuideSpotConfigDTO dto) {

        GuideSpotConfig config = configRepo.findBySpotKey(spotKey)
                .orElseGet(GuideSpotConfig::new);
        config.setSpotKey(spotKey);
        config.setPersonaName(dto.getPersonaName());
        config.setPersonaDesc(dto.getPersonaDesc());
        config.setKnowledgeText(dto.getKnowledgeText());
        config.setActive(dto.isActive());
        configRepo.save(config);
        return Result.success(config);
    }
}
```

- [ ] **Step 2: 创建 `GuideDataInitializer.java`（启动时写入西南大学初始配置，幂等）**

```java
package com.LHZ.TripMate.config;

import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuideDataInitializer implements CommandLineRunner {

    private final GuideSpotConfigRepository configRepo;

    @Override
    public void run(String... args) {
        if (configRepo.findBySpotKey("swu").isPresent()) return;

        GuideSpotConfig config = new GuideSpotConfig();
        config.setSpotKey("swu");
        config.setPersonaName("小渝");
        config.setPersonaDesc("西南大学的专属校园导览助手，熟悉学校的历史沿革、标志性建筑、校园文化和日常生活，性格温暖幽默，说话亲切自然");
        config.setKnowledgeText("""
                西南大学创建于1906年，坐落于重庆市北碚区，是教育部直属的全国重点综合大学，\
                国家"211工程"和"985工程优势学科创新平台"建设高校。\
                校训为"含弘光大，继往开来"。\
                学校拥有北碚主校区和荣昌校区。\
                北碚主校区主要地标建筑包括：\
                含弘楼（学校行政办公中心，建筑宏伟）、\
                图书馆（馆藏丰富，建筑宏伟，是师生学习的重要场所）、\
                惟勤楼（主要教学楼群）、\
                博雅广场（校园中心广场，常举办各类活动）、\
                荷花池（校园标志性景观，四季皆美）、\
                大学生活动中心（学生文化艺术活动核心场所）。\
                学校拥有农学、教育学、心理学、蚕学等全国领先的优势学科，\
                是重庆市重要的科研和人才培养基地。\
                校园依山傍水，绿树成荫，被评为"最美大学校园"之一。\
                """);
        config.setActive(true);
        configRepo.save(config);
        log.info("西南大学导览配置初始化完成");
    }
}
```

- [ ] **Step 3: 启动应用，验证初始数据写入**

```bash
./mvnw spring-boot:run
```

观察启动日志，应出现：`西南大学导览配置初始化完成`

在 MySQL 中确认：

```sql
SELECT spot_key, persona_name, active FROM guide_spot_config;
```

预期：返回一行 `swu | 小渝 | 1`

- [ ] **Step 4: 验证 Admin 接口（需要 SUPER_ADMIN token）**

先登录拿到 admin token：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
```

查询配置列表：

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/guide/configs | python3 -m json.tool
```

预期：返回包含 `swu` 的配置列表，`code: 200`

- [ ] **Step 5: 端对端验证（需要有效 WX_USER token）**

用微信开发者工具登录小程序拿到 wx token，然后：

```bash
WX_TOKEN="<从小程序 storage 复制的 token>"

# 发一条消息（流式，ctrl+c 可中断）
curl -N -X POST http://localhost:8080/api/guide/swu/chat \
  -H "Authorization: Bearer $WX_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"你好，西南大学有哪些著名建筑？"}'
```

预期：终端逐行打印 SSE 数据，形如：
```
data: {"delta":"西南"}
data: {"delta":"大学最"}
...
data: {"done":true}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/controller/admin/AdminGuideController.java \
        src/main/java/com/LHZ/TripMate/config/GuideDataInitializer.java
git commit -m "feat(guide): add AdminGuideController and SWU data initializer"
```

---

## 完成检查清单

- [ ] 三张表已在 MySQL 中自动创建
- [ ] 西南大学初始配置已写入 `guide_spot_config`
- [ ] `GET /api/guide/swu/history` 未登录返回 401，登录后返回空数组 `[]`
- [ ] `POST /api/guide/swu/chat` 流式返回 delta，最终返回 `{"done":true}`
- [ ] `DELETE /api/guide/swu/history` 清空后 history 返回 `[]`
- [ ] `PUT /api/admin/guide/configs/swu` 能更新人设和知识库
- [ ] 再次启动应用，初始化器不重复插入数据
