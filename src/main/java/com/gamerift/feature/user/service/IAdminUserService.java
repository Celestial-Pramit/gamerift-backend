package com.gamerift.feature.user.service;

import java.util.List;

import com.gamerift.feature.user.dto.AdminUserResponse;
import com.gamerift.feature.user.dto.AdminUserUpdateRequest;

/** Admin management of users (list + role/ban + delete). Creation stays with Registration. */
public interface IAdminUserService {

    /** Every user, sorted by username. */
    List<AdminUserResponse> getAll();

    /** Updates role / country / banned. Admins cannot edit their own role/ban nor another admin. */
    AdminUserResponse update(String adminId, String userId, AdminUserUpdateRequest request);

    /**
     * Deletes a user and everything that belongs to them (notifications, reminders,
     * follows and their channels). Admins cannot delete themselves or another admin.
     */
    void delete(String adminId, String userId);
}