package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.config.DeepSeekConfig;
import com.LHZ.TripMate.dto.TripChatRequestDTO;
import com.LHZ.TripMate.dto.TripChatResponseDTO;
import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;
import com.LHZ.TripMate.service.TripChatService;
import com.LHZ.TripMate.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripChatServiceImpl implements TripChatService {

    private final DeepSeekConfig deepSeekConfig;
    private final TtsService ttsService;
    private final ObjectMapper objectMapper;

    private static final int MAX_TTS_LEN = 140;
    private static final int MAX_HISTORY = 6; // 最多带 3 轮历史（6条消息）

    @Override
    public TripChatResponseDTO chat(TripChatRequestDTO request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty()) throw new IllegalArgumentException("消息不能为空");

        String spotName = request.getSpotName() == null ? "当前景区" : request.getSpotName().trim();

        // 构建 messages
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(spotName)));

        // 带入最近历史（不超过 MAX_HISTORY 条）
        if (request.getHistory() != null) {
            List<Map<String, String>> history = request.getHistory();
            int start = Math.max(0, history.size() - MAX_HISTORY);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> h = history.get(i);
                messages.add(Map.of("role", h.getOrDefault("role", "user"),
                        "content", h.getOrDefault("content", "")));
            }
        }
        messages.add(Map.of("role", "user", "content", message));

        // 调用 DeepSeek（同步，非流式）
        String aiText = callDeepSeek(messages);

        // TTS 长度限制
        String ttsText = aiText.length() > MAX_TTS_LEN ? aiText.substring(0, MAX_TTS_LEN) : aiText;

        // 合成语音
        TtsRequestDTO ttsReq = new TtsRequestDTO();
        ttsReq.setText(ttsText);
        TtsResponseDTO ttsRes = ttsService.synthesize(ttsReq);

        return new TripChatResponseDTO(aiText, ttsRes.getAudioUrl());
    }

    private String callDeepSeek(List<Map<String, Object>> messages) {
        RestClient client = RestClient.create();

        Map<String, Object> body = Map.of(
                "model", deepSeekConfig.getModel(),
                "messages", messages,
                "stream", false,
                "max_tokens", 150
        );

        try {
            String responseBody = client.post()
                    .uri(deepSeekConfig.getUrl())
                    .header("Authorization", "Bearer " + deepSeekConfig.getKey())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").path(0).path("message").path("content").asText("抱歉，我暂时无法回答。");
        } catch (Exception e) {
            log.error("DeepSeek call failed", e);
            throw new RuntimeException("AI 服务暂时不可用，请稍后再试");
        }
    }

    private String buildSystemPrompt(String spotName) {
        return String.format(
                "你是%s的智能旅行向导。请用不超过80字的简洁语言回答游客的问题，给出实用的旅行建议和有趣的景点介绍。" +
                "语气亲切自然，像熟悉本地的朋友一样。只用中文回答。",
                spotName
        );
    }
}
