package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import com.LHZ.TripMate.entity.AdminUser;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<AdminUserDTO> listAll() {
        return adminUserRepository.findAll().stream().map(AdminUserDTO::from).toList();
    }

    @Override
    public AdminUserDTO create(CreateAdminUserDTO dto) {
        if (adminUserRepository.existsByUsername(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        AdminUser user = new AdminUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(AdminUser.Role.valueOf(dto.getRole()));
        return AdminUserDTO.from(adminUserRepository.save(user));
    }

    @Override
    public AdminUserDTO update(Long id, UpdateAdminUserDTO dto) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在"));
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return AdminUserDTO.from(adminUserRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!adminUserRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在");
        }
        adminUserRepository.deleteById(id);
    }
}
