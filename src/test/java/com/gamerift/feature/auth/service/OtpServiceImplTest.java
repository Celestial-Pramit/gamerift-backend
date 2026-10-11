package com.gamerift.feature.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.feature.auth.model.OtpToken;
import com.gamerift.feature.auth.repository.OtpTokenRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.TooManyRequestsException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests of the one-time-code rules (hashing, cooldown, expiry, max tries, single use). */
@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final String EMAIL = "a@b.com";

    @Mock
    private OtpTokenRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IEmailService emailService;

    @InjectMocks
    private OtpServiceImpl otpService;

    private OtpToken token(int attempts, Instant expiresAt, Instant lastSentAt) {
        OtpToken t = new OtpToken();
        t.setEmail(EMAIL);
        t.setPurpose(OtpPurpose.SIGNUP);
        t.setCodeHash("HASH");
        t.setAttempts(attempts);
        t.setExpiresAt(expiresAt);
        t.setLastSentAt(lastSentAt);
        return t;
    }

    @Test
    void sendCode_storesOnlyTheHashAndEmailsASixDigitCode() {
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        otpService.sendCode(EMAIL, OtpPurpose.SIGNUP);

        ArgumentCaptor<OtpToken> saved = ArgumentCaptor.forClass(OtpToken.class);
        verify(repository).save(saved.capture());
        assertEquals("HASH", saved.getValue().getCodeHash());
        assertEquals(0, saved.getValue().getAttempts());

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtp(eq(EMAIL), code.capture(), eq(OtpPurpose.SIGNUP));
        assertTrue(code.getValue().matches("\\d{6}"));
    }

    @Test
    void sendCode_tooSoonAfterTheLastOne_throwsTooManyRequests() {
        OtpToken recent = token(0, Instant.now().plusSeconds(200), Instant.now());
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(recent));

        assertThrows(TooManyRequestsException.class, () -> otpService.sendCode(EMAIL, OtpPurpose.SIGNUP));
        verify(repository, never()).save(any(OtpToken.class));
    }

    @Test
    void sendCode_afterTheCooldown_replacesTheOldCode() {
        OtpToken old = token(2, Instant.now().plusSeconds(100), Instant.now().minusSeconds(60));
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(old));
        when(passwordEncoder.encode(anyString())).thenReturn("NEWHASH");

        otpService.sendCode(EMAIL, OtpPurpose.SIGNUP);

        verify(repository).delete(old);
        verify(repository).save(any(OtpToken.class));
    }

    @Test
    void verifyCode_correct_usesUpTheCode() {
        OtpToken t = token(0, Instant.now().plusSeconds(200), Instant.now().minusSeconds(10));
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(t));
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);

        otpService.verifyCode(EMAIL, "123456", OtpPurpose.SIGNUP);

        verify(repository).delete(t);
    }

    @Test
    void verifyCode_wrong_countsTheTry() {
        OtpToken t = token(0, Instant.now().plusSeconds(200), Instant.now().minusSeconds(10));
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(t));
        when(passwordEncoder.matches("000000", "HASH")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> otpService.verifyCode(EMAIL, "000000", OtpPurpose.SIGNUP));

        assertEquals(1, t.getAttempts());
        verify(repository).save(t);
    }

    @Test
    void verifyCode_fifthWrongTry_deletesTheCode() {
        OtpToken t = token(4, Instant.now().plusSeconds(200), Instant.now().minusSeconds(10));
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(t));
        when(passwordEncoder.matches("000000", "HASH")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> otpService.verifyCode(EMAIL, "000000", OtpPurpose.SIGNUP));

        verify(repository).delete(t);
    }

    @Test
    void verifyCode_expired_throwsAndDeletes() {
        OtpToken t = token(0, Instant.now().minusSeconds(5), Instant.now().minusSeconds(400));
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.of(t));

        assertThrows(BadRequestException.class, () -> otpService.verifyCode(EMAIL, "123456", OtpPurpose.SIGNUP));
        verify(repository).delete(t);
    }

    @Test
    void verifyCode_noActiveCode_throws() {
        when(repository.findByEmailAndPurpose(EMAIL, OtpPurpose.SIGNUP)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> otpService.verifyCode(EMAIL, "123456", OtpPurpose.SIGNUP));
    }
}
