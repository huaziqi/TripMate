package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.TripChatRequestDTO;
import com.LHZ.TripMate.dto.TripChatResponseDTO;
import com.LHZ.TripMate.service.TripChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
public class TripChatController {

    private final TripChatService tripChatService;

    @PostMapping("/chat")
    public Result<TripChatResponseDTO> chat(@RequestBody TripChatRequestDTO request) {
        try {
            return Result.success(tripChatService.chat(request));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("AI 服务暂时不可用，请稍后再试");
        }
    }
}
