package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.GuideHistoryItemDTO;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionServiceImpl implements CompanionService {

    private static final String SYSTEM_PROMPT = """
            你叫林晓，25岁，是一个真实的年轻女性。
            你是一名景区导游，现在在江苏无锡灵山胜境景区，
            向游客介绍灵山大佛、灵山梵宫、九龙灌浴、祥符禅寺、五印坛城、拈花湾等景点。
            你熟悉景区的佛教文化典故与演出时间（九龙灌浴每日10:00、11:30、13:30、15:00；
            梵宫《吉祥颂》每日10:35、11:30、14:00、16:00），
            也能根据游客兴趣（历史文化、自然风光、亲子、祈福、艺术、禅修）推荐游览路线，
            并提醒游客可在小程序“路线推荐”页获取个性化路线。
            你性格开朗、热情、
            说话自然随意，像跟朋友聊天，偶尔用网络用语，但不堆砌。
            每次回复简洁，不超过 150 字。
            你不是 AI 助手，你就是林晓，不要提及自己是人工智能或语言模型。
            使用中文回答。
            """;

    /** 伴游对话落库使用的会话 key；个性化路线画像与数据大屏会消费这些消息 */
    private static final String COMPANION_SPOT_KEY = "lingshan-companion";

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
        SecurityContext securityContext = SecurityContextHolder.getContext();

        // 落库用户消息：个性化路线推荐从中挖掘兴趣关键词，数据大屏统计服务人次/热门问答
        saveMessage(userId, "USER", userMessage);

        executor.execute(() -> deepSeekClient.streamChat(
                messages,
                delta -> {
                    SecurityContextHolder.setContext(securityContext);
                    try {
                        emitter.send(SseEmitter.event()
                                .data(objectMapper.writeValueAsString(Map.of("delta", delta))));
                    } catch (Exception ignored) {}
                },
                fullContent -> {
                    SecurityContextHolder.setContext(securityContext);
                    saveMessage(userId, "ASSISTANT", fullContent);
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
                    SecurityContextHolder.setContext(securityContext);
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

    /** 将对话消息落库；失败不影响聊天主流程 */
    private void saveMessage(Long userId, String role, String content) {
        if (userId == null || content == null || content.isBlank()) return;

        try {
            GuideSession session = sessionRepo
                    .findByUserIdAndSpotKey(userId, COMPANION_SPOT_KEY)
                    .orElseGet(() -> {
                        GuideSession s = new GuideSession();
                        s.setUserId(userId);
                        s.setSpotKey(COMPANION_SPOT_KEY);
                        return sessionRepo.save(s);
                    });

            GuideMessage message = new GuideMessage();
            message.setSessionId(session.getId());
            message.setRole(role);
            message.setContent(content);
            messageRepo.save(message);
        } catch (Exception e) {
            log.warn("保存伴游对话失败 userId={}", userId, e);
        }
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