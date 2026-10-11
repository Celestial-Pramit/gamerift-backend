package com.gamerift.feature.user.service;

import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.model.User;

/** Copies a User (database model) into a UserResponse (DTO). The password hash is left out on purpose. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.getCountry(),
                user.getAvatarUrl(), user.getBio(), user.isEmailVerified(), user.isTwoFactorEnabled(), user.isPro(),
                user.getProUntil(), user.getCreatedAt());
    }
}
