package com.gamerift.feature.event.job;

import java.time.Instant;
import java.util.List;

import com.gamerift.feature.event.model.EventReminder;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.notification.service.INotificationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Turns event reminders into notifications 30 minutes before the event starts.
 * Runs every 30 seconds; a reminder is marked "notified" the moment it fires,
 * so it can never fire twice - even if two runs overlap.
 */
@Component
public class EventReminderJob {

    private static final long LEAD_MINUTES = 30;

    private final GameEventRepository eventRepository;
    private final EventReminderRepository reminderRepository;
    private final INotificationService notificationService;

    public EventReminderJob(GameEventRepository eventRepository,
                            EventReminderRepository reminderRepository,
                            INotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 30000)
    public void fire() {
        Instant now = Instant.now();
        List<GameEvent> events = eventRepository.findByStartsAtBetween(now, now.plusSeconds(LEAD_MINUTES * 60));
        for (GameEvent event : events) {
            for (EventReminder reminder : reminderRepository.findByEventIdAndNotifiedFalse(event.getId())) {
                notificationService.send(reminder.getUserId(), "event",
                        event.getTitle() + " starts in " + LEAD_MINUTES + " minutes", "/app/events");
                reminder.setNotified(true);
                reminderRepository.save(reminder);
            }
        }
    }
}