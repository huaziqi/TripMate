package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.wx.UpdateProfileRequestDTO;
import com.LHZ.TripMate.dto.wx.WxLoginRequestDTO;
import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.WxAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
public class WxAuthController {

    private final WxAuthService wxAuthService;

    @PostMapping("/login")
    public Result<WxLoginResponseDTO> login(@Valid @RequestBody WxLoginRequestDTO req) {
        return Result.success(wxAuthService.login(req.getCode()));
    }

    @PostMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO req,
                                      @AuthenticationPrincipal WxUserDetails userDetails) {
        wxAuthService.updateProfile(userDetails.getUsername(), req.getNickname(), req.getAvatarUrl());
        return Result.success();
    }
}
