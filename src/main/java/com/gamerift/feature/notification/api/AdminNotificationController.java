package com.gamerift.feature.notification.api;

import com.gamerift.feature.notification.dto.NotificationRequest;
import com.gamerift.feature.notification.service.IAdminNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only broadcast (admin panel "Broadcast" screen). */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Notifications", description = "Broadcast a notification (ADMIN only)")
public class AdminNotificationController {

    private final IAdminNotificationService adminNotificationService;

    public AdminNotificationController(IAdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send a notification to every user")
    public void broadcast(@Valid @RequestBody NotificationRequest request) {
        adminNotificationService.broadcast(request);
    }
}