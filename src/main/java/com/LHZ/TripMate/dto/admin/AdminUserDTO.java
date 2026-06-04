package com.LHZ.TripMate.dto.admin;

import com.LHZ.TripMate.entity.AdminUser;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUserDTO {
    private Long id;
    private String username;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;

    public static AdminUserDTO from(AdminUser u) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole().name());
        dto.setStatus(u.getStatus());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
}
