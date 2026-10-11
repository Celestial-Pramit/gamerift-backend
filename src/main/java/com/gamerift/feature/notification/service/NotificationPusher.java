package com.gamerift.feature.notification.service;

import com.gamerift.feature.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Pushes a saved notification to the user's WebSocket queue. */
@Component
public class NotificationPusher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPusher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationPusher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Sends the notification DTO to /user/{userId}/queue/notifications. Never throws. */
    public void push(String userId, NotificationResponse dto) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", dto);
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}", userId, e);
        }
    }
}