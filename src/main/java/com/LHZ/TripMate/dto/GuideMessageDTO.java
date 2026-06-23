package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GuideMessageDTO {
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
