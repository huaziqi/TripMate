package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.GuideMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface GuideService {
    List<GuideMessageDTO> getHistory(Long userId, String spotKey);
    SseEmitter chat(String userMessage, String spotKey, Long userId);
    void clearHistory(Long userId, String spotKey);
}
