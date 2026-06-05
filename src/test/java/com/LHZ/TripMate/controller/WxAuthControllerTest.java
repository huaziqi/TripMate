package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.service.WxAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WxAuthControllerTest {

    private MockMvc mockMvc;
    private WxAuthService wxAuthService;

    @BeforeEach
    void setUp() {
        wxAuthService = mock(WxAuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WxAuthController(wxAuthService))
                .build();
    }

    @Test
    void login_withValidCode_returnsToken() throws Exception {
        given(wxAuthService.login("test-code")).willReturn(
                new WxLoginResponseDTO("jwt-token", "openid-123", "用户昵称", "https://avatar.url"));

        mockMvc.perform(post("/api/wx/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.openid").value("openid-123"))
                .andExpect(jsonPath("$.data.nickname").value("用户昵称"));
    }

    @Test
    void login_withBlankCode_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/wx/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_whenServiceThrows_propagatesError() throws Exception {
        given(wxAuthService.login("bad-code")).willThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "微信登录失败: invalid code"));

        mockMvc.perform(post("/api/wx/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad-code\"}"))
                .andExpect(status().isBadRequest());
    }
}
