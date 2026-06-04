package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;
import com.LHZ.TripMate.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return Result.success(adminAuthService.login(request));
    }
}
