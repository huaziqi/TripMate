package com.LHZ.TripMate.dto;

import lombok.Data;

@Data
public class GuideHistoryItemDTO {

    /**
     * USER / ASSISTANT
     */
    private String role;

    private String content;
}
