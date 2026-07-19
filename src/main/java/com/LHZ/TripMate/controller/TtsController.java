package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;
import com.LHZ.TripMate.service.TtsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping("/synthesize")
    public Result<TtsResponseDTO> synthesize(@Valid @RequestBody TtsRequestDTO request) {
        return Result.success(ttsService.synthesize(request));
    }

    @PostMapping("/synthesize-with-timeline")
    public Result<TtsResponseDTO> synthesizeWithTimeline(@Valid @RequestBody TtsRequestDTO request) {
        return Result.success(ttsService.synthesizeWithTimeline(request));
    }
}