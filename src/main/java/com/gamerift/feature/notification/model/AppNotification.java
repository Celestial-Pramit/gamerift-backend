package com.gamerift.feature.notification.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One notification for one user (collection "notifications").
 * type: live | event | reply | pay | follow | system.
 * targetUrl is where the bell navigates (for example /app/settings after a payment).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "notifications")
public class AppNotification {

    @Id
    private String id;

    private String userId;
    private String type;
    private String title;
    private String targetUrl;
    private boolean read;
    private Instant createdAt = Instant.now();
}