package com.gamerift.feature.notification.api;

import java.util.List;

import com.gamerift.feature.notification.dto.NotificationResponse;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.stream.dto.ToggleRequest;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for the user's own notifications.
 * Every command is scoped to the logged-in user (the service checks ownership).
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Read, mark and delete the logged-in user's notifications")
public class NotificationController {

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "My notifications, newest first")
    public List<NotificationResponse> getAll(@AuthenticationPrincipal AuthUser authUser) {
        return notificationService.getNotifications(authUser.getId());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark one notification read or unread")
    public NotificationResponse setRead(@PathVariable String id, @RequestBody ToggleRequest request,
                                        @AuthenticationPrincipal AuthUser authUser) {
        return notificationService.setRead(authUser.getId(), id, request.isEnabled());
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark every notification read")
    public void markAllRead(@AuthenticationPrincipal AuthUser authUser) {
        notificationService.markAllRead(authUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one notification")
    public void delete(@PathVariable String id, @AuthenticationPrincipal AuthUser authUser) {
        notificationService.delete(authUser.getId(), id);
    }
}