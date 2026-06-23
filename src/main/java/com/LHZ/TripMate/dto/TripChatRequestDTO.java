package com.LHZ.TripMate.dto;

import java.util.List;
import java.util.Map;

public class TripChatRequestDTO {

    private String message;
    private String spotName;
    // 前端传最近几轮对话，格式 [{role:"user",content:"..."},{role:"assistant",content:"..."}]
    private List<Map<String, String>> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }

    public List<Map<String, String>> getHistory() { return history; }
    public void setHistory(List<Map<String, String>> history) { this.history = history; }
}
