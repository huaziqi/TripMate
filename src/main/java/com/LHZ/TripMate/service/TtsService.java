package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;
import jakarta.validation.Valid;

public interface TtsService {

    TtsResponseDTO synthesize(TtsRequestDTO request);

    TtsResponseDTO synthesizeWithTimeline(TtsRequestDTO request);
}