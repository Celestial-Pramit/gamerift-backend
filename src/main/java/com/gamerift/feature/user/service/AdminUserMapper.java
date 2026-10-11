package com.gamerift.feature.user.service;

import com.gamerift.feature.user.dto.AdminUserResponse;
import com.gamerift.feature.user.model.User;

/** Copies a User into an AdminUserResponse. Pure mapping, no logic. */
public final class AdminUserMapper {

    private AdminUserMapper() {
    }

    static AdminUserResponse toResponse(User user) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());
        r.setCountry(user.getCountry());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setBanned(user.isBanned());
        r.setPro(user.isPro());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}