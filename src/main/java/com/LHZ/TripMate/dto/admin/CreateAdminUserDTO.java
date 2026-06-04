package com.LHZ.TripMate.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAdminUserDTO {
    @NotBlank private String username;
    @NotBlank private String password;
    @Pattern(regexp = "SUPER_ADMIN|ADMIN") private String role;
}
