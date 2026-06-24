package com.LHZ.TripMate.dto;

import lombok.Data;

import java.util.List;

@Data
public class GuideChatRequestDTO {
    private String message;
    private List<GuideHistoryItemDTO> history;

}
