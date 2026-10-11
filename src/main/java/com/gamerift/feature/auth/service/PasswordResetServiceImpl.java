package com.gamerift.feature.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.gamerift.feature.auth.model.PasswordResetToken;
import com.gamerift.feature.auth.repository.PasswordResetTokenRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.util.HashUtil;
import com.gamerift.shared.util.RateLimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Forgot-password flow:
 *  1. user asks with an e-mail -> we ALWAYS answer "if that e-mail exists, we sent a link"
 *  2. we create a random token (32 bytes), store only its SHA-256 hash, valid 15 minutes, single use
 *  3. the e-mail contains a link to the React page with the token
 *  4. the user sends the token + new password -> we check the hash, change the password, delete the token
 */
@Service
public class PasswordResetServiceImpl implements IPasswordResetService {

    private static final String INVALID = "This reset link is not valid or has expired";

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final RateLimiter rateLimiter;
    private final String frontendUrl;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    IEmailService emailService,
                                    RateLimiter rateLimiter,
                                    @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void requestReset(String emailInput) {
        String email = emailInput.trim().toLowerCase();
        // at most 3 requests per 10 minutes for the same e-mail
        rateLimiter.check("forgot:" + email, 3, Duration.ofMinutes(10), "Too many reset requests. Please try again later");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isBanned()) {
            return; // silent: same answer as for a real account
        }
        // one active link per user
        tokenRepository.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(HashUtil.sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(15 * 60));
        tokenRepository.save(token);

        emailService.sendPasswordReset(email, frontendUrl + "/reset-password?token=" + rawToken);
    }

    @Override
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(HashUtil.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException(INVALID));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new BadRequestException(INVALID);
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException(INVALID));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // single use
        tokenRepository.delete(token);
    }
}
