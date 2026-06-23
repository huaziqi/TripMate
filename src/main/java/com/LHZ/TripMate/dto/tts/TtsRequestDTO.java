package com.LHZ.TripMate.dto.tts;

import jakarta.validation.constraints.NotBlank;

public class TtsRequestDTO {

    @NotBlank(message = "合成文本不能为空")
    private String text;

    public TtsRequestDTO() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}