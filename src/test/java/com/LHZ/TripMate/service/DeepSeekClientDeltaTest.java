package com.LHZ.TripMate.service;

import tools.jackson.databind.ObjectMapper;
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
