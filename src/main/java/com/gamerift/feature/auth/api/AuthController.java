package com.gamerift.feature.auth.api;

import com.gamerift.feature.auth.dto.AuthResponse;
import com.gamerift.feature.auth.dto.EmailRequest;
import com.gamerift.feature.auth.dto.LoginRequest;
import com.gamerift.feature.auth.dto.RegisterRequest;
import com.gamerift.feature.auth.dto.RegisterResponse;
import com.gamerift.feature.auth.dto.ResetPasswordRequest;
import com.gamerift.feature.auth.dto.VerifyOtpRequest;
import com.gamerift.feature.auth.service.IAuthService;
import com.gamerift.feature.auth.service.IPasswordResetService;
import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.shared.dto.MessageResponse;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for register, e-mail verification, login (+ two-step), password reset and "who am I".
 * It only receives the request, calls a service and returns a DTO; all rules live in the services.
 *
 * Login with two-step verification:
 *   POST /login            -> {needsOtp:true}  (a code is e-mailed)
 *   POST /login/verify-otp -> {token, user}
 *
 * @Valid runs the validation rules of the request DTO.
 * @AuthenticationPrincipal gives us the logged-in user that JwtAuthFilter put into the security context.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Register, verify, login, two-step verification, password reset")
public class AuthController {

    private final IAuthService authService;
    private final IPasswordResetService passwordResetService;

    public AuthController(IAuthService authService, IPasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a viewer account and e-mail a 6-digit code")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the e-mail with the code from registration")
    public MessageResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifySignupOtp(request);
        return new MessageResponse("Email verified. You can log in now.");
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Send a new registration code")
    public MessageResponse resendOtp(@Valid @RequestBody EmailRequest request) {
        authService.resendSignupOtp(request.getEmail());
        return new MessageResponse("If the account needs verification, a new code was sent.");
    }

    @PostMapping("/login")
    @Operation(summary = "Log in (step 1). Returns a token, or needsOtp=true when a code was e-mailed")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/verify-otp")
    @Operation(summary = "Log in (step 2): send the e-mailed code, get the token")
    public AuthResponse verifyLoginOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyLoginOtp(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "E-mail a reset link (the answer is the same whether or not the e-mail exists)")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return new MessageResponse("If that email exists, a reset link is on its way.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Choose a new password with the token from the e-mail link")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getPassword());
        return new MessageResponse("Password changed. You can log in now.");
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user (needs a token)")
    public UserResponse me(@AuthenticationPrincipal AuthUser authUser) {
        return authService.getCurrentUser(authUser.getId());
    }
}
