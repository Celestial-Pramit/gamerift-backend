package com.gamerift.feature.auth.service;

import java.time.Duration;

import com.gamerift.feature.auth.dto.AuthResponse;
import com.gamerift.feature.auth.dto.LoginRequest;
import com.gamerift.feature.auth.dto.RegisterRequest;
import com.gamerift.feature.auth.dto.RegisterResponse;
import com.gamerift.feature.auth.dto.VerifyOtpRequest;
import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.feature.user.service.UserMapper;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.NotFoundException;
import com.gamerift.shared.exception.TooManyRequestsException;
import com.gamerift.shared.exception.UnauthorizedException;
import com.gamerift.shared.security.JwtUtil;
import com.gamerift.shared.util.RateLimiter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business rules of register, e-mail verification and login (layer: service).
 *
 * Login has two steps when needed:
 *   1. e-mail + password  -> if the user is ADMIN or turned on two-step verification, a code is e-mailed
 *   2. e-mail + code      -> the token is issued
 * Dependencies come through the constructor (constructor injection).
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IOtpService otpService;
    private final RateLimiter rateLimiter;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           IOtpService otpService, RateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        // 1. clean the input (emails are stored in lower case so "A@x.com" and "a@x.com" are the same)
        String email = normalize(request.getEmail());
        String username = request.getUsername().trim();

        // 2. business rules: email and username must be unique
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken");
        }

        // 3. build the user; only the BCrypt HASH of the password is stored; the e-mail is NOT verified yet
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCountry(request.getCountry());
        user.setRole(Role.VIEWER);
        user.setEmailVerified(false);
        userRepository.save(user);

        // 4. e-mail the 6-digit code
        otpService.sendCode(email, OtpPurpose.SIGNUP);
        return new RegisterResponse(email, "Account created. Check your email for the 6-digit code.");
    }

    @Override
    public void verifySignupOtp(VerifyOtpRequest request) {
        String email = normalize(request.getEmail());
        rateLimiter.check("verify:" + email, 10, Duration.ofMinutes(10), "Too many tries. Please wait and try again");

        // same message for "no such account" and "wrong code": no information leaks
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));
        otpService.verifyCode(email, request.getCode(), OtpPurpose.SIGNUP);

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendSignupOtp(String emailInput) {
        String email = normalize(emailInput);
        rateLimiter.check("resend:" + email, 5, Duration.ofMinutes(10), "Too many requests. Please wait and try again");

        User user = userRepository.findByEmail(email).orElse(null);
        // silent when the account does not exist or is already verified
        if (user != null && !user.isEmailVerified()) {
            otpService.sendCode(email, OtpPurpose.SIGNUP);
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.getEmail());
        // protects against password guessing: 10 tries per minute per e-mail
        rateLimiter.check("login:" + email, 10, Duration.ofMinutes(1), "Too many login attempts. Please wait a minute");

        // 1. find the user. Unknown email and wrong password give the SAME message.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // 2. banned users cannot log in
        if (user.isBanned()) {
            throw new ForbiddenException("This account has been banned");
        }

        // 3. the e-mail must be verified first (we send a fresh code to help the user)
        if (!user.isEmailVerified()) {
            try {
                otpService.sendCode(email, OtpPurpose.SIGNUP);
            } catch (TooManyRequestsException ignored) {
                // a code was sent a moment ago: nothing to do
            }
            throw new ForbiddenException("Email not verified. We sent you a new code");
        }

        // 4. two-step verification: always for ADMIN, optional for viewers
        if (user.getRole() == Role.ADMIN || user.isTwoFactorEnabled()) {
            try {
                otpService.sendCode(email, OtpPurpose.LOGIN);
            } catch (TooManyRequestsException ignored) {
                // a login code was sent a moment ago: the user can use it
            }
            return AuthResponse.otpRequired();
        }

        // 5. normal login
        return issueToken(user);
    }

    @Override
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        String email = normalize(request.getEmail());
        rateLimiter.check("verify:" + email, 10, Duration.ofMinutes(10), "Too many tries. Please wait and try again");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));
        if (user.isBanned()) {
            throw new ForbiddenException("This account has been banned");
        }
        otpService.verifyCode(email, request.getCode(), OtpPurpose.LOGIN);
        return issueToken(user);
    }

    @Override
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserMapper.toResponse(user);
    }

    private AuthResponse issueToken(User user) {
        return AuthResponse.success(jwtUtil.generateToken(user), UserMapper.toResponse(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
