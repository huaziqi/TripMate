package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.entity.GuideMessage;
import com.LHZ.TripMate.entity.GuideSession;
import com.LHZ.TripMate.entity.GuideSpotConfig;
import com.LHZ.TripMate.entity.KnowledgeDoc;
import com.LHZ.TripMate.entity.KnowledgeSpotEntry;
import com.LHZ.TripMate.repository.GuideMessageRepository;
import com.LHZ.TripMate.repository.GuideSessionRepository;
import com.LHZ.TripMate.repository.GuideSpotConfigRepository;
import com.LHZ.TripMate.repository.KnowledgeDocRepository;
import com.LHZ.TripMate.repository.KnowledgeSpotEntryRepository;
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

    /** 知识库注入 system prompt 的总字符上限，防止超长文档撑爆上下文 */
    private static final int KNOWLEDGE_CHAR_LIMIT = 20_000;

    /** 景点结构化条目注入的字符上限 */
    private static final int SPOT_ENTRY_CHAR_LIMIT = 15_000;

    private final GuideSpotConfigRepository spotConfigRepo;
    private final GuideSessionRepository sessionRepo;
    private final GuideMessageRepository messageRepo;
    private final KnowledgeDocRepository knowledgeDocRepo;
    private final KnowledgeSpotEntryRepository knowledgeSpotRepo;
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
                %s
                ## 对话规范
                - 回答简洁自然，每次不超过 150 字
                - 不知道的内容直接说"这个我还不太清楚"，不编造
                - 保持导游视角，语气亲切，适当引导游客探索
                - 使用中文回答
                """, config.getPersonaName(), config.getPersonaDesc(),
                config.getKnowledgeText(),
                buildSpotEntrySection(config.getSpotKey()) + buildKnowledgeSection(config.getSpotKey()));
    }

    /** 拼接景点结构化知识条目：每个景点一段，仅输出非空字段 */
    private String buildSpotEntrySection(String spotKey) {
        List<KnowledgeSpotEntry> entries = knowledgeSpotRepo.findBySpotKeyAndEnabledTrueOrderBySpotCode(spotKey);
        if (entries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("\n## 景点详细资料\n");
        for (KnowledgeSpotEntry entry : entries) {
            if (sb.length() >= SPOT_ENTRY_CHAR_LIMIT) {
                sb.append("\n（篇幅所限，其余景点资料略）\n");
                break;
            }
            sb.append("\n### ").append(entry.getName()).append('（').append(entry.getSpotCode()).append('）').append('\n');
            appendField(sb, "所属景区", entry.getZoneName());
            appendField(sb, "具体位置", entry.getLocation());
            appendField(sb, "建筑/景观参数", entry.getScaleInfo());
            appendField(sb, "核心功能", entry.getCoreFunction());
            appendField(sb, "文化内涵", entry.getCulture());
            appendField(sb, "详细介绍", entry.getDescription());
            appendField(sb, "游玩亮点", entry.getTourTips());
            appendField(sb, "演艺/开放信息", entry.getTicketInfo());
            appendField(sb, "备注", entry.getRemark());
        }
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append('：').append(value).append('\n');
        }
    }

    /** 拼接知识库中该景点可用的知识文档（专属 + 通用），按分类分组，总量限长 */
    private String buildKnowledgeSection(String spotKey) {
        List<KnowledgeDoc> docs = knowledgeDocRepo.findActiveKnowledgeForSpot(spotKey);
        if (docs.isEmpty()) return "";

        Map<KnowledgeDoc.Category, String> categoryLabels = Map.of(
                KnowledgeDoc.Category.EXPLANATION, "讲解词",
                KnowledgeDoc.Category.HISTORY, "文史资料",
                KnowledgeDoc.Category.FAQ, "常见问题及答案",
                KnowledgeDoc.Category.OTHER, "其他资料");

        StringBuilder sb = new StringBuilder();
        KnowledgeDoc.Category currentCategory = null;
        for (KnowledgeDoc doc : docs) {
            if (sb.length() >= KNOWLEDGE_CHAR_LIMIT) {
                sb.append("\n（篇幅所限，其余知识文档略）\n");
                break;
            }
            if (doc.getCategory() != currentCategory) {
                currentCategory = doc.getCategory();
                sb.append("\n## ").append(categoryLabels.get(currentCategory)).append('\n');
            }
            sb.append("\n### ").append(doc.getTitle()).append('\n');
            String content = doc.getContent();
            int remaining = KNOWLEDGE_CHAR_LIMIT - sb.length();
            if (content.length() > remaining) {
                sb.append(content, 0, Math.max(0, remaining)).append("\n（本文档过长，已截断）\n");
            } else {
                sb.append(content).append('\n');
            }
        }
        return sb.toString();
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
