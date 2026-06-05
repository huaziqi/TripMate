package com.LHZ.TripMate.dto.wx;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WxLoginResponseDTO {
    private String token;
    private String openid;
    private String nickname;
    private String avatarUrl;
}
