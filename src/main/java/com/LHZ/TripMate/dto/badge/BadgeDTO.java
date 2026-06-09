package com.LHZ.TripMate.dto.badge;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BadgeDTO {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String rarity;
    private String icon;
    private String unlockCondition;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private String note;
}
