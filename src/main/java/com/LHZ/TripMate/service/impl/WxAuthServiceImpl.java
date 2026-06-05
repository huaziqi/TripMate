package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.config.WxConfig;
import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.WxAuthService;
import com.LHZ.TripMate.util.JwtUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class WxAuthServiceImpl implements WxAuthService {

    private final WxConfig wxConfig;
    private final WxUserRepository wxUserRepository;
    private final JwtUtil jwtUtil;
    private final RestClient restClient;

    public WxAuthServiceImpl(WxConfig wxConfig,
                              WxUserRepository wxUserRepository,
                              JwtUtil jwtUtil) {
        this.wxConfig = wxConfig;
        this.wxUserRepository = wxUserRepository;
        this.jwtUtil = jwtUtil;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .build();
    }

    @Override
    public WxLoginResponseDTO login(String code) {
        String openid = fetchOpenid(code);

        WxUser wxUser = wxUserRepository.findByOpenid(openid)
                .orElseGet(() -> {
                    WxUser newUser = new WxUser();
                    newUser.setOpenid(openid);
                    newUser.setNickname("");
                    newUser.setAvatarUrl("");
                    return wxUserRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(openid, "WX_USER", "WX_USER");
        return new WxLoginResponseDTO(
                token,
                openid,
                wxUser.getNickname() != null ? wxUser.getNickname() : "",
                wxUser.getAvatarUrl() != null ? wxUser.getAvatarUrl() : ""
        );
    }

    @Override
    public void updateProfile(String openid, String nickname, String avatarUrl) {
        WxUser wxUser = wxUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (nickname != null) {
            wxUser.setNickname(nickname);
        }
        if (avatarUrl != null) {
            wxUser.setAvatarUrl(avatarUrl);
        }
        wxUserRepository.save(wxUser);
    }

    private String fetchOpenid(String code) {
        Jscode2SessionResponse res = restClient.get()
                .uri(b -> b.path("/sns/jscode2session")
                           .queryParam("appid", wxConfig.getAppid())
                           .queryParam("secret", wxConfig.getSecret())
                           .queryParam("js_code", code)
                           .queryParam("grant_type", "authorization_code")
                           .build())
                .retrieve()
                .body(Jscode2SessionResponse.class);

        if (res == null || (res.getErrcode() != null && res.getErrcode() != 0)) {
            String msg = res != null ? res.getErrmsg() : "无响应";
            log.warn("微信 jscode2session 失败: {}", msg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信登录失败: " + msg);
        }
        return res.getOpenid();
    }

    @Data
    private static class Jscode2SessionResponse {
        private String openid;
        @JsonProperty("session_key")
        private String sessionKey;
        private Integer errcode;
        private String errmsg;
    }
}
