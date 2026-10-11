package com.gamerift.feature.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.gamerift.feature.auth.dto.AuthResponse;
import com.gamerift.feature.auth.dto.LoginRequest;
import com.gamerift.feature.auth.dto.RegisterRequest;
import com.gamerift.feature.auth.dto.VerifyOtpRequest;
import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.ForbiddenException;
import com.gamerift.shared.exception.TooManyRequestsException;
import com.gamerift.shared.exception.UnauthorizedException;
import com.gamerift.shared.security.JwtUtil;
import com.gamerift.shared.util.RateLimiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests of the auth SERVICE. Repository, password encoder, JWT helper and OTP service are fakes (mocks);
 * the rate limiter is a real object (@Spy). We test only OUR rules.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private IOtpService otpService;
    @Spy
    private RateLimiter rateLimiter = new RateLimiter();

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("gamer1");
        r.setEmail("Gamer@Example.com");
        r.setPassword("Passw0rd!");
        r.setCountry("Bangladesh");
        return r;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail("gamer@example.com");
        r.setPassword(password);
        return r;
    }

    private VerifyOtpRequest verifyRequest(String code) {
        VerifyOtpRequest r = new VerifyOtpRequest();
        r.setEmail("gamer@example.com");
        r.setCode(code);
        return r;
    }

    private User storedUser(Role role, boolean verified, boolean banned, boolean twoFactor) {
        User u = new User();
        u.setId("u1");
        u.setUsername("gamer1");
        u.setEmail("gamer@example.com");
        u.setPasswordHash("HASH");
        u.setRole(role);
        u.setEmailVerified(verified);
        u.setBanned(banned);
        u.setTwoFactorEnabled(twoFactor);
        return u;
    }

    // ---------------------------------------------------------------- register

    @Test
    void register_savesUnverifiedUserWithHashAndSendsCode() {
        when(userRepository.existsByEmail("gamer@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("gamer1")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASH");

        authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("gamer@example.com", saved.getEmail());
        assertEquals("HASH", saved.getPasswordHash());
        assertEquals(Role.VIEWER, saved.getRole());
        assertFalse(saved.isEmailVerified());
        verify(otpService).sendCode("gamer@example.com", OtpPurpose.SIGNUP);
    }

    @Test
    void register_duplicateEmail_throwsConflictAndSavesNothing() {
        when(userRepository.existsByEmail("gamer@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest()));
        verify(userRepository, never()).save(any(User.class));
    }

    // ------------------------------------------------------------------- login

    @Test
    void login_verifiedViewerWithoutTwoFactor_returnsToken() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.VIEWER, true, false, false)));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("TOKEN");

        AuthResponse response = authService.login(loginRequest("Passw0rd!"));

        assertEquals("TOKEN", response.getToken());
        assertFalse(response.isNeedsOtp());
    }

    @Test
    void login_admin_alwaysNeedsOtpAndNoTokenYet() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.ADMIN, true, false, false)));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);

        AuthResponse response = authService.login(loginRequest("Passw0rd!"));

        assertTrue(response.isNeedsOtp());
        verify(otpService).sendCode("gamer@example.com", OtpPurpose.LOGIN);
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void login_viewerWithTwoFactor_needsOtp() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.VIEWER, true, false, true)));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);

        assertTrue(authService.login(loginRequest("Passw0rd!")).isNeedsOtp());
    }

    @Test
    void login_unverifiedEmail_throwsForbiddenAndSendsNewCode() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.VIEWER, false, false, false)));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> authService.login(loginRequest("Passw0rd!")));
        verify(otpService).sendCode("gamer@example.com", OtpPurpose.SIGNUP);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.VIEWER, true, false, false)));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest("wrong")));
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest("Passw0rd!")));
    }

    @Test
    void login_bannedUser_throwsForbidden() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.VIEWER, true, true, false)));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> authService.login(loginRequest("Passw0rd!")));
    }

    @Test
    void login_eleventhAttemptInOneMinute_isRateLimited() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.empty());

        for (int i = 0; i < 10; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest("x")));
        }
        assertThrows(TooManyRequestsException.class, () -> authService.login(loginRequest("x")));
    }

    // --------------------------------------------------------------------- OTP

    @Test
    void verifySignupOtp_marksTheEmailAsVerified() {
        User user = storedUser(Role.VIEWER, false, false, false);
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(user));

        authService.verifySignupOtp(verifyRequest("123456"));

        verify(otpService).verifyCode("gamer@example.com", "123456", OtpPurpose.SIGNUP);
        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
    }

    @Test
    void verifyLoginOtp_correctCode_returnsToken() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.ADMIN, true, false, false)));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("TOKEN");

        AuthResponse response = authService.verifyLoginOtp(verifyRequest("123456"));

        verify(otpService).verifyCode("gamer@example.com", "123456", OtpPurpose.LOGIN);
        assertEquals("TOKEN", response.getToken());
    }

    @Test
    void verifyLoginOtp_wrongCode_givesNoToken() {
        when(userRepository.findByEmail("gamer@example.com")).thenReturn(Optional.of(storedUser(Role.ADMIN, true, false, false)));
        doThrow(new BadRequestException("Wrong code")).when(otpService).verifyCode("gamer@example.com", "000000", OtpPurpose.LOGIN);

        assertThrows(BadRequestException.class, () -> authService.verifyLoginOtp(verifyRequest("000000")));
        verify(jwtUtil, never()).generateToken(any(User.class));
    }
}
