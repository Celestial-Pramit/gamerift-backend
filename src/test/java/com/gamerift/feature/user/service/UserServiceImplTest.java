package com.gamerift.feature.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user(Role role) {
        User u = new User();
        u.setId("u1");
        u.setUsername("gamer1");
        u.setEmail("a@b.com");
        u.setPasswordHash("OLD");
        u.setRole(role);
        return u;
    }

    @Test
    void setTwoFactor_viewerCanTurnItOn() {
        User u = user(Role.VIEWER);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        UserResponse response = userService.setTwoFactor("u1", true);

        assertTrue(u.isTwoFactorEnabled());
        assertTrue(response.getTwoFactor());
        verify(userRepository).save(u);
    }

    @Test
    void setTwoFactor_adminCannotTurnItOff() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThrows(BadRequestException.class, () -> userService.setTwoFactor("u1", false));
    }

    @Test
    void changePassword_wrongCurrentPassword_isRejected() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user(Role.VIEWER)));
        when(passwordEncoder.matches("bad", "OLD")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changePassword("u1", "bad", "NewPass1"));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void changePassword_correctCurrentPassword_storesTheNewHash() {
        User u = user(Role.VIEWER);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("OldPass1", "OLD")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1")).thenReturn("NEWHASH");

        userService.changePassword("u1", "OldPass1", "NewPass1");

        assertEquals("NEWHASH", u.getPasswordHash());
        verify(userRepository).save(u);
    }
}
