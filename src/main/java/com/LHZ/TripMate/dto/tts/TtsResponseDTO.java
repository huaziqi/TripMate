package com.LHZ.TripMate.dto.tts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TtsResponseDTO {

    private String audioUrl;
    private String sessionId;
    private Long duration;
    private List<PhonemeItemDTO> phonemes;
    private List<VisemeItemDTO> visemes;


    public TtsResponseDTO(String audioUrl, String sessionId) {
        this.audioUrl = audioUrl;
        this.sessionId = sessionId;
    }
}