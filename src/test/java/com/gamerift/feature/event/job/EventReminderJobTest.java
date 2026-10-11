package com.gamerift.feature.event.job;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.gamerift.feature.event.model.EventReminder;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.notification.service.INotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** R5 (gap requirement): the reminder job fires 30 minutes before an event, once per reminder. */
@ExtendWith(MockitoExtension.class)
class EventReminderJobTest {

    @Mock
    private GameEventRepository eventRepository;
    @Mock
    private EventReminderRepository reminderRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private EventReminderJob job;

    private GameEvent event(String id, String title) {
        GameEvent e = new GameEvent();
        e.setId(id);
        e.setTitle(title);
        return e;
    }

    private EventReminder reminder(String id, String userId, String eventId) {
        EventReminder r = new EventReminder();
        r.setId(id);
        r.setUserId(userId);
        r.setEventId(eventId);
        return r;
    }

    @Test
    void fire_sendsOneNotificationPerReminderAndMarksItNotified() {
        GameEvent e = event("e1", "Cup Finals");
        EventReminder r = reminder("r1", "u1", "e1");
        when(eventRepository.findByStartsAtBetween(any(), any())).thenReturn(List.of(e));
        when(reminderRepository.findByEventIdAndNotifiedFalse("e1")).thenReturn(List.of(r));

        job.fire();

        verify(notificationService).send("u1", "event", "Cup Finals starts in 30 minutes", "/app/events");
        assertTrue(r.isNotified());
        verify(reminderRepository).save(r);
    }

    @Test
    void fire_noEventsInTheWindow_doesNothing() {
        when(eventRepository.findByStartsAtBetween(any(), any())).thenReturn(List.of());

        job.fire();

        verify(notificationService, never()).send(any(), any(), any(), any());
        verify(reminderRepository, never()).findByEventIdAndNotifiedFalse(any());
    }
}