package com.gamerift.feature.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.gamerift.feature.user.dto.NotificationPrefsDto;
import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.DisplayName;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests of the profile and alert-switch rules. The repository is a fake. */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user() {
        User u = new User();
        u.setId("u1");
        u.setUsername("gamer");
        u.setEmail("gamer@example.com");
        u.setCountry("India");
        return u;
    }

    @Test
    @DisplayName("updateProfile saves bio and country")
    void updateProfile_savesBioAndCountry() {
        User u = user();
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        UserResponse response = userService.updateProfile("u1", "Hello", "Bangladesh");

        assertEquals("Hello", response.getBio());
        assertEquals("Bangladesh", response.getCountry());
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("updateProfile trims text and turns a missing bio into empty text")
    void updateProfile_trimsAndHandlesMissingBio() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user()));

        UserResponse response = userService.updateProfile("u1", null, "  Pakistan ");

        assertEquals("", response.getBio());
        assertEquals("Pakistan", response.getCountry());
    }

    @Test
    @DisplayName("updateProfile for an unknown user throws NotFound")
    void updateProfile_unknownUser_throwsNotFound() {
        when(userRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateProfile("zzz", "x", "India"));
    }

    @Test
    @DisplayName("alert switches are all on for a new user")
    void notificationPrefs_defaultAllOn() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(user()));

        NotificationPrefsDto prefs = userService.getNotificationPrefs("u1");

        assertTrue(prefs.isLive());
        assertTrue(prefs.isEvents());
        assertTrue(prefs.isReplies());
        assertTrue(prefs.isPay());
    }

    @Test
    @DisplayName("setNotificationPrefs saves each switch")
    void setNotificationPrefs_savesEachSwitch() {
        User u = user();
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        NotificationPrefsDto request = new NotificationPrefsDto();
        request.setLive(false);
        request.setEvents(true);
        request.setReplies(false);
        request.setPay(true);

        NotificationPrefsDto saved = userService.setNotificationPrefs("u1", request);

        assertFalse(saved.isLive());
        assertTrue(saved.isEvents());
        assertFalse(u.isNotifyReplies());
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("setNotificationPrefs for an unknown user throws NotFound")
    void setNotificationPrefs_unknownUser_throwsNotFound() {
        when(userRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.setNotificationPrefs("zzz", new NotificationPrefsDto()));
    }
}