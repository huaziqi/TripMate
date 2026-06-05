package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;

public interface WxAuthService {
    WxLoginResponseDTO login(String code);
    void updateProfile(String openid, String nickname, String avatarUrl);
}
