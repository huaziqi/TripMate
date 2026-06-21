package com.LHZ.TripMate.dto;

import lombok.Data;

@Data
public class TranslationRequestDTO {
    private String text;
    private String from;
    private String to;
}
