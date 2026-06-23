package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.tts.TtsRequestDTO;
import com.LHZ.TripMate.dto.tts.TtsResponseDTO;

public interface TtsService {

    TtsResponseDTO synthesize(TtsRequestDTO request);
}