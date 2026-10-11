package com.gamerift.feature.user.service;

import com.gamerift.feature.user.dto.NotificationPrefsDto;
import com.gamerift.feature.user.dto.UserResponse;

/** Settings of the logged-in user. */
public interface IUserService {

    /** Turns two-step verification on or off. Admins cannot turn it off. */
    UserResponse setTwoFactor(String userId, boolean enabled);

    /** Changes the password after checking the current one. */
    void changePassword(String userId, String currentPassword, String newPassword);

    /** Saves bio and country. A missing bio becomes an empty text. */
    UserResponse updateProfile(String userId, String bio, String country);

    /** The four alert switches of this user. */
    NotificationPrefsDto getNotificationPrefs(String userId);

    /** Saves the four alert switches and returns them. */
    NotificationPrefsDto setNotificationPrefs(String userId, NotificationPrefsDto prefs);
}