package com.gamerift.shared.security;

import com.gamerift.feature.user.model.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The logged-in user as Spring Security knows it (the "principal").
 * JwtAuthFilter creates it for every valid request; controllers receive it with
 * @AuthenticationPrincipal AuthUser authUser.
 */
@Getter
@AllArgsConstructor
public class AuthUser {

    private final String id;
    private final String email;
    private final Role role;
}
