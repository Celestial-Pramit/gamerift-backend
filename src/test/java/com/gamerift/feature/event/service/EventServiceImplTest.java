package com.gamerift.feature.event.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.model.EventReminder;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the event rules (tabs, status, Remind me). Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private GameEventRepository eventRepository;
    @Mock
    private EventReminderRepository reminderRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private GameEvent event(String id, String category, long hoursFromNow) {
        GameEvent e = new GameEvent();
        e.setId(id);
        e.setTitle("Event " + id);
        e.setCategory(category);
        e.setStartsAt(Instant.now().plus(hoursFromNow, ChronoUnit.HOURS));
        return e;
    }

    private List<GameEvent> fourEvents() {
        return List.of(event("past", "ESPORTS", -10), event("live", "ESPORTS", -1), event("later", "IRL", 5), event("soon", "ESPORTS", 2));
    }

    @Test
    void upcomingTab_returnsFutureEventsSoonestFirst() {
        when(eventRepository.findAll()).thenReturn(fourEvents());

        List<EventResponse> result = eventService.getAll("u1", "upcoming", null);

        assertEquals(2, result.size());
        assertEquals("soon", result.get(0).getId());
        assertEquals("later", result.get(1).getId());
    }

    @Test
    void liveTab_returnsEventsThatStartedWithinThreeHours() {
        when(eventRepository.findAll()).thenReturn(fourEvents());

        List<EventResponse> result = eventService.getAll("u1", "live", null);

        assertEquals(1, result.size());
        assertEquals("live", result.get(0).getStatus());
    }

    @Test
    void pastTab_returnsFinishedEvents() {
        when(eventRepository.findAll()).thenReturn(fourEvents());

        List<EventResponse> result = eventService.getAll("u1", "past", null);

        assertEquals(1, result.size());
        assertEquals("past", result.get(0).getId());
    }

    @Test
    void remindersTab_returnsOnlyTheEventsTheUserIsRemindedAbout() {
        EventReminder reminder = new EventReminder();
        reminder.setUserId("u1");
        reminder.setEventId("later");
        when(eventRepository.findAll()).thenReturn(fourEvents());
        when(reminderRepository.findByUserId("u1")).thenReturn(List.of(reminder));

        List<EventResponse> result = eventService.getAll("u1", "reminders", null);

        assertEquals(1, result.size());
        assertEquals("later", result.get(0).getId());
        assertTrue(result.get(0).isReminded());
    }

    @Test
    void categoryFilter_ignoresUpperAndLowerCase() {
        when(eventRepository.findAll()).thenReturn(fourEvents());

        List<EventResponse> result = eventService.getAll("u1", "all", "irl");

        assertEquals(1, result.size());
        assertEquals("later", result.get(0).getId());
    }

    @Test
    void unknownTab_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> eventService.getAll("u1", "tomorrow", null));
    }

    @Test
    void setReminder_on_savesTheReminder() {
        when(eventRepository.findById("soon")).thenReturn(Optional.of(event("soon", "ESPORTS", 2)));
        when(reminderRepository.existsByUserIdAndEventId("u1", "soon")).thenReturn(false);

        EventResponse response = eventService.setReminder("u1", "soon", true);

        verify(reminderRepository).save(any(EventReminder.class));
        assertTrue(response.isReminded());
    }

    @Test
    void setReminder_onTwice_doesNotSaveAgain() {
        when(eventRepository.findById("soon")).thenReturn(Optional.of(event("soon", "ESPORTS", 2)));
        when(reminderRepository.existsByUserIdAndEventId("u1", "soon")).thenReturn(true);

        eventService.setReminder("u1", "soon", true);

        verify(reminderRepository, never()).save(any(EventReminder.class));
    }

    @Test
    void setReminder_off_deletesTheReminder() {
        when(eventRepository.findById("soon")).thenReturn(Optional.of(event("soon", "ESPORTS", 2)));
        when(reminderRepository.existsByUserIdAndEventId("u1", "soon")).thenReturn(true);

        EventResponse response = eventService.setReminder("u1", "soon", false);

        verify(reminderRepository).deleteByUserIdAndEventId("u1", "soon");
        assertFalse(response.isReminded());
    }

    @Test
    void setReminder_forAnEventThatIsOver_throwsBadRequest() {
        when(eventRepository.findById("past")).thenReturn(Optional.of(event("past", "ESPORTS", -10)));

        assertThrows(BadRequestException.class, () -> eventService.setReminder("u1", "past", true));
    }

    @Test
    void setReminder_unknownEvent_throwsNotFound() {
        when(eventRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.setReminder("u1", "zzz", true));
    }

    @Test
    void time_isShownInBangladeshTime() {
        GameEvent e = new GameEvent();
        e.setId("t");
        e.setStartsAt(Instant.parse("2026-10-20T14:30:00Z"));

        // 14:30 UTC = 20:30 in Bangladesh (UTC+6)
        assertEquals("20:30 BDT", EventMapper.toResponse(e, "upcoming", false).getTime());
    }
}
