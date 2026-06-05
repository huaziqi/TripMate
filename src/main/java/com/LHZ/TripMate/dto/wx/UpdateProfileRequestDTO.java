package com.LHZ.TripMate.dto.wx;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    private String nickname;
    private String avatarUrl;
}
