package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.TranslationResponseDTO;

public interface TranslationService {
    TranslationResponseDTO translate(String text, String from, String to);
}
