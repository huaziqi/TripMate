package com.LHZ.TripMate.dto;

public class HistoryAddRequestDTO {

    private String type;
    private Long targetId;
    private String content;

    public HistoryAddRequestDTO() {
    }

    public String getType() {
        return type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getContent() {
        return content;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setContent(String content) {
        this.content = content;
    }
}