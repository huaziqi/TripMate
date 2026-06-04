package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import java.util.List;

public interface AdminUserService {
    List<AdminUserDTO> listAll();
    AdminUserDTO create(CreateAdminUserDTO dto);
    AdminUserDTO update(Long id, UpdateAdminUserDTO dto);
    void delete(Long id);
}
