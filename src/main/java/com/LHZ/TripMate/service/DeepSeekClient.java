package com.LHZ.TripMate.service;

import com.LHZ.TripMate.config.DeepSeekConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
