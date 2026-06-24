package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.GuideChatRequestDTO;
import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.CompanionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companion")
@RequiredArgsConstructor
public class CompanionChatController {

    private final CompanionService companionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/history")
    public Result<List<GuideMessageDTO>> history(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(companionService.getHistory(userDetails.getWxUser().getId()));
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestBody GuideChatRequestDTO request,
            @AuthenticationPrincipal WxUserDetails userDetails) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", "消息不能为空"))));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        try {
            return companionService.chat(
                    request.getMessage().trim(),
                    request.getHistory(),
                    userDetails.getWxUser().getId());
        } catch (RuntimeException e) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", e.getMessage()))));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }
    }

    @DeleteMapping("/history")
    public Result<Void> clearHistory(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        companionService.clearHistory(userDetails.getWxUser().getId());
        return Result.success(null);
    }
}
