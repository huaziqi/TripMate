package com.LHZ.TripMate.dto.wx;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    @Size(max = 64)
    private String nickname;

    @Size(max = 512)
    private String avatarUrl;
}
