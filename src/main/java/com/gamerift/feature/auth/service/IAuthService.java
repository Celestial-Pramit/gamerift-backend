package com.gamerift.feature.auth.service;

import com.gamerift.feature.auth.dto.AuthResponse;
import com.gamerift.feature.auth.dto.LoginRequest;
import com.gamerift.feature.auth.dto.RegisterRequest;
import com.gamerift.feature.auth.dto.RegisterResponse;
import com.gamerift.feature.auth.dto.VerifyOtpRequest;
import com.gamerift.feature.user.dto.UserResponse;

/** What the auth feature can do (the controller depends on this interface). */
public interface IAuthService {

    RegisterResponse register(RegisterRequest request);

    /** Checks the e-mail code sent at registration and marks the e-mail as verified. */
    void verifySignupOtp(VerifyOtpRequest request);

    /** Sends a new registration code (only if the account exists and is not verified yet). */
    void resendSignupOtp(String email);

    /** Step 1 of login (password). Returns a token, or "needsOtp" when two-step verification applies. */
    AuthResponse login(LoginRequest request);

    /** Step 2 of login (e-mail code) for admins and users who turned on two-step verification. */
    AuthResponse verifyLoginOtp(VerifyOtpRequest request);

    UserResponse getCurrentUser(String userId);
}
