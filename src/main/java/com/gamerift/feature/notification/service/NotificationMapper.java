package com.gamerift.feature.notification.service;

import com.gamerift.feature.notification.dto.NotificationResponse;
import com.gamerift.feature.notification.model.AppNotification;

/** Copies an AppNotification into a NotificationResponse. Pure mapping, no logic. */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    static NotificationResponse toResponse(AppNotification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setTo(n.getTargetUrl());
        r.setRead(n.isRead());
        r.setTime(n.getCreatedAt().toEpochMilli());
        return r;
    }
}