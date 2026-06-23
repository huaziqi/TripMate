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
import tools.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
                    List<GuideMessage> msgs = new ArrayList<>(
                            messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()));
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
        List<GuideMessage> history = new ArrayList<>(
                messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()));
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
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("guide onComplete error", e);
                            emitter.complete();
                        }
                    },
                    error -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(objectMapper.writeValueAsString(Map.of("error", error))));
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("guide onError send failed", e);
                            emitter.complete();
                        }
                    }
            );
        });

        return emitter;
    }

    @Override
    @Transactional
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
