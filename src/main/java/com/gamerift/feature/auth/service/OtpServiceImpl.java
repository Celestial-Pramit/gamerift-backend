package com.gamerift.feature.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.feature.auth.model.OtpToken;
import com.gamerift.feature.auth.repository.OtpTokenRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.TooManyRequestsException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Rules for one-time codes:
 *  - 6 digits made with SecureRandom (not guessable),
 *  - only the BCrypt hash is stored (like a password),
 *  - valid 5 minutes, at most 5 wrong tries, one active code per e-mail + purpose,
 *  - a new code can be asked only every 30 seconds.
 */
@Service
public class OtpServiceImpl implements IOtpService {

    static final long VALID_SECONDS = 300;
    static final long RESEND_SECONDS = 30;
    static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    public OtpServiceImpl(OtpTokenRepository otpTokenRepository, PasswordEncoder passwordEncoder, IEmailService emailService) {
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public void sendCode(String email, OtpPurpose purpose) {
        Instant now = Instant.now();

        // 1. cooldown: do not allow a new code too quickly
        Optional<OtpToken> existing = otpTokenRepository.findByEmailAndPurpose(email, purpose);
        if (existing.isPresent() && existing.get().getLastSentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
            throw new TooManyRequestsException("Please wait a few seconds before asking for a new code");
        }
        // 2. only one active code: remove the old one
        if (existing.isPresent()) {
            otpTokenRepository.delete(existing.get());
        }

        // 3. make a 6-digit code (leading zeros allowed) and store its hash
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setPurpose(purpose);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setAttempts(0);
        token.setLastSentAt(now);
        token.setExpiresAt(now.plusSeconds(VALID_SECONDS));
        otpTokenRepository.save(token);

        // 4. send the real code by e-mail (in the background)
        emailService.sendOtp(email, code, purpose);
    }

    @Override
    public void verifyCode(String email, String code, OtpPurpose purpose) {
        OtpToken token = otpTokenRepository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new BadRequestException("No active code. Please ask for a new one"));

        // expired (MongoDB removes old ones within a minute, but we check anyway)
        if (token.getExpiresAt().isBefore(Instant.now())) {
            otpTokenRepository.delete(token);
            throw new BadRequestException("This code has expired. Please ask for a new one");
        }
        // too many wrong tries
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            otpTokenRepository.delete(token);
            throw new BadRequestException("Too many wrong tries. Please ask for a new code");
        }
        // wrong code: count the try
        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= MAX_ATTEMPTS) {
                otpTokenRepository.delete(token);
            } else {
                otpTokenRepository.save(token);
            }
            throw new BadRequestException("Wrong code");
        }
        // correct: a code can be used only once
        otpTokenRepository.delete(token);
    }
}
