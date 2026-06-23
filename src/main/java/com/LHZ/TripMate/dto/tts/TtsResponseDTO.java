package com.LHZ.TripMate.dto.tts;

public class TtsResponseDTO {

    private String audioUrl;
    private String sessionId;

    public TtsResponseDTO() {
    }

    public TtsResponseDTO(String audioUrl, String sessionId) {
        this.audioUrl = audioUrl;
        this.sessionId = sessionId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}