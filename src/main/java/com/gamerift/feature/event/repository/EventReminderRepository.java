package com.gamerift.feature.event.repository;

import java.util.List;

import com.gamerift.feature.event.model.EventReminder;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventReminderRepository extends MongoRepository<EventReminder, String> {

    List<EventReminder> findByUserId(String userId);

    List<EventReminder> findByEventIdAndNotifiedFalse(String eventId);

    boolean existsByUserIdAndEventId(String userId, String eventId);

    // derived delete: removes the matching document(s)
    void deleteByUserIdAndEventId(String userId, String eventId);

    // derived delete: removes all reminders of one event (used when an admin deletes the event)
    void deleteByEventId(String eventId);

    // derived delete: removes all reminders of one user (used when an admin deletes the account)
    void deleteByUserId(String userId);
}
