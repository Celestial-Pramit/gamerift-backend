package com.gamerift.feature.user.service;

import com.gamerift.feature.user.dto.NotificationPrefsDto;
import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business rules for the settings of the logged-in user (layer: service).
 * Called by: UserController. Calls: UserRepository, PasswordEncoder.
 * Rules: admins always use two-step verification; the password change needs the current password;
 * the profile only changes bio and country (never role, email or ban status).
 */
@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse setTwoFactor(String userId, boolean enabled) {
        User user = findUser(userId);
        // rule: admins always use two-step verification
        if (user.getRole() == Role.ADMIN && !enabled) {
            throw new BadRequestException("Admins always use two-step verification");
        }
        user.setTwoFactorEnabled(enabled);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is wrong");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public UserResponse updateProfile(String userId, String bio, String country) {
        // 1. the user must exist
        User user = findUser(userId);
        // 2. clean the input: no spaces around the text, a missing bio becomes ""
        user.setBio(bio == null ? "" : bio.trim());
        user.setCountry(country.trim());
        // 3. save and answer with the new profile
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Override
    public NotificationPrefsDto getNotificationPrefs(String userId) {
        return toPrefs(findUser(userId));
    }

    @Override
    public NotificationPrefsDto setNotificationPrefs(String userId, NotificationPrefsDto prefs) {
        User user = findUser(userId);
        user.setNotifyLive(prefs.isLive());
        user.setNotifyEvents(prefs.isEvents());
        user.setNotifyReplies(prefs.isReplies());
        user.setNotifyPay(prefs.isPay());
        userRepository.save(user);
        return toPrefs(user);
    }

    private NotificationPrefsDto toPrefs(User user) {
        NotificationPrefsDto dto = new NotificationPrefsDto();
        dto.setLive(user.isNotifyLive());
        dto.setEvents(user.isNotifyEvents());
        dto.setReplies(user.isNotifyReplies());
        dto.setPay(user.isNotifyPay());
        return dto;
    }

    private User findUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}