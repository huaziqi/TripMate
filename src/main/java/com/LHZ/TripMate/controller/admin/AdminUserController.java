package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import com.LHZ.TripMate.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<List<AdminUserDTO>> list() {
        return Result.success(adminUserService.listAll());
    }

    @PostMapping
    public Result<AdminUserDTO> create(@Valid @RequestBody CreateAdminUserDTO dto) {
        return Result.success(adminUserService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<AdminUserDTO> update(@PathVariable Long id,
                                       @RequestBody UpdateAdminUserDTO dto) {
        return Result.success(adminUserService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.success();
    }
}
