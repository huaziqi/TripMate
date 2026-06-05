package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.common.ResultCode;
import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;
import com.LHZ.TripMate.entity.AdminUser;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.service.AdminAuthService;
import com.LHZ.TripMate.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ResultCode.USER_NOT_FOUND.getMessage()));

        if (user.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ResultCode.PASSWORD_ERROR.getMessage());
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), "ADMIN");
        return new LoginResponseDTO(token, user.getRole().name(), user.getUsername());
    }
}
