package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslationResponseDTO {
    private String translatedText;
    private String detectedLang;
    private String from;
    private String to;
}
