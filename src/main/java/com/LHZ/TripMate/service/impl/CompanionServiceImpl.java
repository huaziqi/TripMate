package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.GuideHistoryItemDTO;
import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
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

    private static final String SYSTEM_PROMPT = """
            你叫林晓，25岁，是一个真实的年轻女性。
            你是一名景区导游，现在在重庆北碚西南大学，
            向游客们推荐介绍附近的旅游景点
            你性格开朗、热情、
            说话自然随意，像跟朋友聊天，偶尔用网络用语，但不堆砌。
            每次回复简洁，不超过 150 字。
            你不是 AI 助手，你就是林晓，不要提及自己是人工智能或语言模型。
            使用中文回答。
            """;

    private final GuideSessionRepository sessionRepo;
    private final GuideMessageRepository messageRepo;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_HISTORY_ITEMS = 36;
    private static final int MAX_HISTORY_CHARS = 3600;
    @Autowired
    @Qualifier("guideTaskExecutor")
    private Executor executor;

    @Override
    public List<GuideMessageDTO> getHistory(Long userId) {
        // 暂时禁用后端历史读取
        return List.of();
    }

    @Override
    public SseEmitter chat(String userMessage, List<GuideHistoryItemDTO> history, Long userId) {
        List<Map<String, Object>> messages = buildMessages(history, userMessage);

        SseEmitter emitter = new SseEmitter(120_000L);

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
        // 暂时禁用清空历史逻辑
    }

    private List<Map<String, Object>> buildMessages(List<GuideHistoryItemDTO> history, String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        if (history != null && !history.isEmpty()) {
            List<GuideHistoryItemDTO> trimmedHistory =
                    history.size() > MAX_HISTORY_ITEMS
                            ? history.subList(history.size() - MAX_HISTORY_ITEMS, history.size())
                            : history;

            int totalChars = 0;
            LinkedList<Map<String, Object>> temp = new LinkedList<>();

            for (int i = trimmedHistory.size() - 1; i >= 0; i--) {
                GuideHistoryItemDTO item = trimmedHistory.get(i);
                if (item == null || item.getRole() == null || item.getContent() == null) continue;
                if (item.getContent().isBlank()) continue;

                String role = item.getRole().trim().toLowerCase();
                if (!role.equals("user") && !role.equals("assistant")) continue;

                String content = item.getContent().trim();
                if (!temp.isEmpty() && totalChars + content.length() > MAX_HISTORY_CHARS) {
                    break;
                }

                totalChars += content.length();
                temp.addFirst(Map.of("role", role, "content", content));
            }

            messages.addAll(temp);
        }

        messages.add(Map.of("role", "user", "content", userMessage));
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