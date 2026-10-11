package com.gamerift.feature.notification.service;

import com.gamerift.feature.notification.dto.NotificationRequest;

/** Admin: send a notification to every user. */
public interface IAdminNotificationService {

    /** One notification per user, honouring each user's alert switches. */
    void broadcast(NotificationRequest request);
}