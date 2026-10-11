package com.gamerift.feature.user.dto;

import java.time.Instant;

import com.gamerift.feature.user.model.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the admin "Users" screen. */
@Getter
@Setter
@NoArgsConstructor
public class AdminUserResponse {

    private String id;
    private String username;
    private String email;
    private Role role;
    private String country;
    private String avatarUrl;
    private boolean banned;
    private boolean pro;
    private Instant createdAt;
}