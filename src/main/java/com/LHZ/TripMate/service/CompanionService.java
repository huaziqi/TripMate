package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface CompanionService {
    List<GuideMessageDTO> getHistory(Long userId);
    SseEmitter chat(String userMessage, Long userId);
    void clearHistory(Long userId);
}
