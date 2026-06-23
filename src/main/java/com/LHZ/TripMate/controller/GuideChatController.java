package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.GuideChatRequestDTO;
import com.LHZ.TripMate.dto.GuideMessageDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideChatController {

    private final GuideService guideService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{spotKey}/history")
    public Result<List<GuideMessageDTO>> history(
            @PathVariable String spotKey,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(guideService.getHistory(userDetails.getWxUser().getId(), spotKey));
    }

    @PostMapping(value = "/{spotKey}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @PathVariable String spotKey,
            @RequestBody GuideChatRequestDTO request,
            @AuthenticationPrincipal WxUserDetails userDetails) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of("error", "消息不能为空"))));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        try {
            return guideService.chat(
                    request.getMessage().trim(),
                    spotKey,
                    userDetails.getWxUser().getId());
        } catch (RuntimeException e) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of("error", e.getMessage()))));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }
    }

    @DeleteMapping("/{spotKey}/history")
    public Result<Void> clearHistory(
            @PathVariable String spotKey,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        guideService.clearHistory(userDetails.getWxUser().getId(), spotKey);
        return Result.success(null);
    }
}
