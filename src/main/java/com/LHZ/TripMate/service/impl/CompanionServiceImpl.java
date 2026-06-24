package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.repository.GuideMessageRepository;
import com.LHZ.TripMate.repository.GuideSessionRepository;
import com.LHZ.TripMate.service.CompanionService;
import com.LHZ.TripMate.service.DeepSeekClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionServiceImpl implements CompanionService {

    // 全局会话与景点会话共用同一张表，用此 key 隔离
    private static final String COMPANION_KEY = "__companion__";

    private static final String SYSTEM_PROMPT = """
            你叫林晓，25岁，是一个真实的年轻女性。
            性格开朗、热情、有点小幽默，偶尔会卖萌，但不矫情。
            平时喜欢旅行、拍照、看书、喝奶茶，有自己的想法和观点。
            说话自然随意，像跟朋友聊天，偶尔用网络用语，但不堆砌。
            每次回复简洁，不超过 150 字。
            你不是 AI 助手，你就是林晓，不要提及自己是人工智能或语言模型。
            使用中文回答。
            """;

    private final GuideSessionRepository sessionRepo;
    private final GuideMessageRepository messageRepo;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier("guideTaskExecutor")
    private Executor executor;

    @Override
    public List<GuideMessageDTO> getHistory(Long userId) {
        return sessionRepo.findByUserIdAndSpotKey(userId, COMPANION_KEY)
                .map(session -> {
                    List<GuideMessage> msgs = new ArrayList<>(
                            messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()));
                    Collections.reverse(msgs);
                    return msgs.stream().map(this::toDTO).toList();
                })
                .orElse(List.of());
    }

    @Override
    public SseEmitter chat(String userMessage, Long userId) {
        GuideSession session = sessionRepo.findByUserIdAndSpotKey(userId, COMPANION_KEY)
                .orElseGet(() -> {
                    GuideSession s = new GuideSession();
                    s.setUserId(userId);
                    s.setSpotKey(COMPANION_KEY);
                    return sessionRepo.save(s);
                });

        GuideMessage userMsg = new GuideMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("USER");
        userMsg.setContent(userMessage);
        messageRepo.save(userMsg);

        List<GuideMessage> history = new ArrayList<>(
                messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()));
        Collections.reverse(history);

        List<Map<String, Object>> messages = buildMessages(history);
        SseEmitter emitter = new SseEmitter(120_000L);
        final Long sessionId = session.getId();

        executor.execute(() -> deepSeekClient.streamChat(
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
                        log.warn("companion onComplete error", e);
                        emitter.complete();
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(objectMapper.writeValueAsString(Map.of("error", error))));
                        emitter.complete();
                    } catch (Exception e) {
                        log.warn("companion onError send failed", e);
                        emitter.complete();
                    }
                }
        ));

        return emitter;
    }

    @Override
    @Transactional
    public void clearHistory(Long userId) {
        sessionRepo.findByUserIdAndSpotKey(userId, COMPANION_KEY)
                .ifPresent(session -> messageRepo.deleteBySessionId(session.getId()));
    }

    private List<Map<String, Object>> buildMessages(List<GuideMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
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
