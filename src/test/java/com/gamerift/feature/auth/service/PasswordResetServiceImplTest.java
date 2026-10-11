package com.gamerift.feature.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.gamerift.feature.auth.model.PasswordResetToken;
import com.gamerift.feature.auth.repository.PasswordResetTokenRepository;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.TooManyRequestsException;
import com.gamerift.shared.util.HashUtil;
import com.gamerift.shared.util.RateLimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests of forgot / reset password. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IEmailService emailService;

    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(userRepository, tokenRepository, passwordEncoder, emailService,
                new RateLimiter(), "http://localhost:5173");
    }

    private User user() {
        User u = new User();
        u.setId("u1");
        u.setEmail("a@b.com");
        u.setPasswordHash("OLD");
        return u;
    }

    @Test
    void requestReset_unknownEmail_doesNothingAndRevealsNothing() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        service.requestReset("A@B.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestReset_knownEmail_storesOnlyTheHashAndEmailsTheLink() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user()));

        service.requestReset("a@b.com");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(org.mockito.ArgumentMatchers.eq("a@b.com"), link.capture());

        assertTrue(link.getValue().startsWith("http://localhost:5173/reset-password?token="));
        String rawToken = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        assertEquals(HashUtil.sha256Hex(rawToken), saved.getValue().getTokenHash());
        assertNotEquals(rawToken, saved.getValue().getTokenHash());
        verify(tokenRepository).deleteByUserId("u1");
    }

    @Test
    void requestReset_fourthRequestInTenMinutes_isRateLimited() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            service.requestReset("a@b.com");
        }
        assertThrows(TooManyRequestsException.class, () -> service.requestReset("a@b.com"));
    }

    @Test
    void resetPassword_validToken_changesPasswordAndDeletesTheToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("u1");
        token.setExpiresAt(Instant.now().plusSeconds(600));
        User user = user();
        when(tokenRepository.findByTokenHash(HashUtil.sha256Hex("RAW"))).thenReturn(Optional.of(token));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass1")).thenReturn("NEWHASH");

        service.resetPassword("RAW", "NewPass1");

        assertEquals("NEWHASH", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPassword_unknownToken_throwsBadRequest() {
        when(tokenRepository.findByTokenHash(HashUtil.sha256Hex("BAD"))).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.resetPassword("BAD", "NewPass1"));
    }

    @Test
    void resetPassword_expiredToken_throwsBadRequestAndDeletesIt() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("u1");
        token.setExpiresAt(Instant.now().minusSeconds(5));
        when(tokenRepository.findByTokenHash(HashUtil.sha256Hex("OLDTOKEN"))).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> service.resetPassword("OLDTOKEN", "NewPass1"));
        verify(tokenRepository).delete(token);
    }
}
