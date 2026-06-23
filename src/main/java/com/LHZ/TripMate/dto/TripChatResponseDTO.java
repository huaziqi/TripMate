package com.LHZ.TripMate.dto;

public class TripChatResponseDTO {

    private String text;
    private String audioUrl;

    public TripChatResponseDTO() {}

    public TripChatResponseDTO(String text, String audioUrl) {
        this.text = text;
        this.audioUrl = audioUrl;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}
