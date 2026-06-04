package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;

public interface AdminAuthService {
    LoginResponseDTO login(LoginRequestDTO request);
}
