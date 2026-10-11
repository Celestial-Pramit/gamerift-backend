package com.gamerift.feature.event.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * "User X wants a reminder for event Y" (collection "event_reminders").
 * The unique compound index stops the same reminder being stored twice.
 * notified = the reminder has already become a notification (the job marks it so it only fires once).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "event_reminders")
@CompoundIndex(name = "user_event", def = "{'userId': 1, 'eventId': 1}", unique = true)
public class EventReminder {

    @Id
    private String id;

    private String userId;
    private String eventId;
    private boolean notified;
    private Instant createdAt = Instant.now();
}
