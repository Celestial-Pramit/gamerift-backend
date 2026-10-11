package com.gamerift.feature.auth.dto;

import com.gamerift.feature.user.dto.UserResponse;

import lombok.Getter;

/**
 * Answer of login steps. Two shapes:
 *  - success:      { token, user, needsOtp: false }
 *  - code needed:  { token: null, user: null, needsOtp: true }  (two-step verification: the React app shows the OTP screen)
 */
@Getter
public class AuthResponse {

    private final String token;
    private final UserResponse user;
    private final boolean needsOtp;

    private AuthResponse(String token, UserResponse user, boolean needsOtp) {
        this.token = token;
        this.user = user;
        this.needsOtp = needsOtp;
    }

    public static AuthResponse success(String token, UserResponse user) {
        return new AuthResponse(token, user, false);
    }

    public static AuthResponse otpRequired() {
        return new AuthResponse(null, null, true);
    }
}
