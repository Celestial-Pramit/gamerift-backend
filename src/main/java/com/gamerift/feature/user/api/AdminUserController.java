package com.gamerift.feature.user.api;

import java.util.List;

import com.gamerift.feature.user.dto.AdminUserResponse;
import com.gamerift.feature.user.dto.AdminUserUpdateRequest;
import com.gamerift.feature.user.service.IAdminUserService;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only management of users (admin panel "Users" screen). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Users", description = "List users and change role / ban status (ADMIN only)")
public class AdminUserController {

    private final IAdminUserService adminUserService;

    public AdminUserController(IAdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "All users, sorted by username")
    public List<AdminUserResponse> getAll() {
        return adminUserService.getAll();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role, country or ban status of one user")
    public AdminUserResponse update(@PathVariable String id, @RequestBody AdminUserUpdateRequest request,
                                    @AuthenticationPrincipal AuthUser admin) {
        return adminUserService.update(admin.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user and everything that belongs to them")
    public void delete(@PathVariable String id, @AuthenticationPrincipal AuthUser admin) {
        adminUserService.delete(admin.getId(), id);
    }
}