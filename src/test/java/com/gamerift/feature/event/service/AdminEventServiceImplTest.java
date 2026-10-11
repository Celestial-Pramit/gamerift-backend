package com.gamerift.feature.event.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.event.dto.EventRequest;
import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin event rules (money check, upper-case category, delete with reminders). Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminEventServiceImplTest {

    @Mock
    private GameEventRepository eventRepository;
    @Mock
    private EventReminderRepository reminderRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private AdminEventServiceImpl service;

    private GameEvent event(String id, long hoursFromNow) {
        GameEvent e = new GameEvent();
        e.setId(id);
        e.setTitle("Event " + id);
        e.setCategory("ESPORTS");
        e.setStartsAt(Instant.now().plus(hoursFromNow, ChronoUnit.HOURS));
        return e;
    }

    private EventRequest request(String category, long goal, long raised) {
        EventRequest r = new EventRequest();
        r.setTitle("  Cup  ");
        r.setCategory(category);
        r.setStartsAt(Instant.now().plus(5, ChronoUnit.HOURS));
        r.setGoal(goal);
        r.setRaised(raised);
        return r;
    }

    @Test
    void getAll_newestStartTimeFirst() {
        when(eventRepository.findAll()).thenReturn(List.of(event("old", -10), event("future", 20), event("mid", 2)));

        List<EventResponse> result = service.getAll();

        assertEquals("future", result.get(0).getId());
        assertEquals("mid", result.get(1).getId());
        assertEquals("old", result.get(2).getId());
    }

    @Test
    void create_trimsTitleAndUpperCasesCategory() {
        when(eventRepository.save(any(GameEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = service.create(request("irl", 0, 0));

        assertEquals("Cup", response.getTitle());
        assertEquals("IRL", response.getCategory());
        assertEquals("upcoming", response.getStatus());
    }

    @Test
    void create_tellsEveryoneAboutTheNewEvent() {
        when(eventRepository.save(any(GameEvent.class))).thenAnswer(inv -> {
            GameEvent e = inv.getArgument(0);
            e.setId("e1");
            return e;
        });

        service.create(request("ESPORTS", 0, 0));

        verify(notificationService).notifyEventCreated(eq("e1"), eq("Cup"), eq(null));
    }

    @Test
    void create_raisedMoreThanGoal_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.create(request("ESPORTS", 1000, 1500)));
        verify(eventRepository, never()).save(any(GameEvent.class));
    }

    @Test
    void create_noGoal_ignoresTheMoneyRule() {
        when(eventRepository.save(any(GameEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = service.create(request("ESPORTS", 0, 500));

        assertEquals(500, response.getRaised());
    }

    @Test
    void update_changesTheFields() {
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event("e1", 3)));
        when(eventRepository.save(any(GameEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = service.update("e1", request("workshop", 100, 50));

        assertEquals("WORKSHOP", response.getCategory());
        assertEquals(100, response.getGoal());
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(eventRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update("zzz", request("IRL", 0, 0)));
    }

    @Test
    void delete_removesRemindersThenTheEvent() {
        when(eventRepository.existsById("e1")).thenReturn(true);

        service.delete("e1");

        verify(reminderRepository).deleteByEventId("e1");
        verify(eventRepository).deleteById("e1");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(eventRepository.existsById("zzz")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.delete("zzz"));
        verify(reminderRepository, never()).deleteByEventId(any());
    }
}
