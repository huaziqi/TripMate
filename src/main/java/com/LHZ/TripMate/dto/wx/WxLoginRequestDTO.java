package com.LHZ.TripMate.dto.wx;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequestDTO {
    @NotBlank
    private String code;
}
