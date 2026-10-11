package com.gamerift.feature.user.dto;

import com.gamerift.feature.user.model.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/admin/users/{id}. role can stay null (keeps the current role). */
@Getter
@Setter
@NoArgsConstructor
public class AdminUserUpdateRequest {

    private Role role;
    private String country;
    private boolean banned;
}