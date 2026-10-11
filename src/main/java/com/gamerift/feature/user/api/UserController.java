package com.gamerift.feature.user.api;

import com.gamerift.feature.user.dto.ChangePasswordRequest;
import com.gamerift.feature.user.dto.NotificationPrefsDto;
import com.gamerift.feature.user.dto.ProfileRequest;
import com.gamerift.feature.user.dto.TwoFactorRequest;
import com.gamerift.feature.user.dto.UserResponse;
import com.gamerift.feature.user.service.IUserService;
import com.gamerift.shared.dto.MessageResponse;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Settings of the logged-in user. Needs a token (these URLs are not in the public list of SecurityConfig).
 * Request flow of "Save profile": Browser -> UserController (@Valid) -> IUserService (UserServiceImpl)
 *   -> UserRepository -> MongoDB -> UserResponse JSON.
 */
@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Users", description = "Settings of the logged-in user")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    @Operation(summary = "Save bio and country")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody ProfileRequest request) {
        return userService.updateProfile(authUser.getId(), request.getBio(), request.getCountry());
    }

    @PutMapping("/2fa")
    @Operation(summary = "Turn two-step verification on or off")
    public UserResponse setTwoFactor(@AuthenticationPrincipal AuthUser authUser, @RequestBody TwoFactorRequest request) {
        return userService.setTwoFactor(authUser.getId(), request.isEnabled());
    }

    @PutMapping("/password")
    @Operation(summary = "Change the password")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return new MessageResponse("Password changed");
    }

    @GetMapping("/notification-prefs")
    @Operation(summary = "The four alert switches (live, events, replies, pay)")
    public NotificationPrefsDto getNotificationPrefs(@AuthenticationPrincipal AuthUser authUser) {
        return userService.getNotificationPrefs(authUser.getId());
    }

    @PutMapping("/notification-prefs")
    @Operation(summary = "Save the four alert switches")
    public NotificationPrefsDto setNotificationPrefs(@AuthenticationPrincipal AuthUser authUser, @RequestBody NotificationPrefsDto request) {
        return userService.setNotificationPrefs(authUser.getId(), request);
    }
}