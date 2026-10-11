package com.gamerift.feature.event.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.model.EventReminder;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for events (layer: service).
 *  - the status (upcoming / live / past) is calculated from the start time, never stored,
 *  - the tabs of the page are filters on that status ("reminders" = events this user wants a reminder for),
 *  - "Remind me" is idempotent and not allowed for events that are already over.
 */
@Service
public class EventServiceImpl implements IEventService {

    private static final Set<String> TABS = Set.of("all", "upcoming", "live", "past", "reminders");

    private final GameEventRepository eventRepository;
    private final EventReminderRepository reminderRepository;

    public EventServiceImpl(GameEventRepository eventRepository, EventReminderRepository reminderRepository) {
        this.eventRepository = eventRepository;
        this.reminderRepository = reminderRepository;
    }

    @Override
    public List<EventResponse> getAll(String userId, String tab, String category) {
        // 1. check the tab word
        String wanted = (tab == null || tab.isBlank()) ? "all" : tab.trim().toLowerCase();
        if (!TABS.contains(wanted)) {
            throw new BadRequestException("tab must be all, upcoming, live, past or reminders");
        }

        Set<String> remindedIds = remindedEventIds(userId);
        Instant now = Instant.now();
        List<EventResponse> result = new ArrayList<>();

        // 2. look at every event and keep the ones that match the tab and the category
        for (GameEvent event : eventRepository.findAll()) {
            if (category != null && !category.isBlank() && !category.trim().equalsIgnoreCase(event.getCategory())) {
                continue;
            }
            String status = EventMapper.statusOf(event, now);
            boolean reminded = remindedIds.contains(event.getId());

            boolean show;
            if (wanted.equals("all")) {
                show = true;
            } else if (wanted.equals("reminders")) {
                show = reminded;
            } else {
                show = status.equals(wanted);
            }
            if (show) {
                result.add(EventMapper.toResponse(event, status, reminded));
            }
        }

        // 3. past events: newest first; everything else: soonest first
        if (wanted.equals("past")) {
            result.sort((a, b) -> b.getStartsAt().compareTo(a.getStartsAt()));
        } else {
            result.sort((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()));
        }
        return result;
    }

    @Override
    public EventResponse setReminder(String userId, String eventId, boolean enabled) {
        GameEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        String status = EventMapper.statusOf(event, Instant.now());

        if (enabled && status.equals("past")) {
            throw new BadRequestException("This event is already over");
        }
        boolean exists = reminderRepository.existsByUserIdAndEventId(userId, eventId);
        if (enabled && !exists) {
            EventReminder reminder = new EventReminder();
            reminder.setUserId(userId);
            reminder.setEventId(eventId);
            reminderRepository.save(reminder);
        }
        if (!enabled && exists) {
            reminderRepository.deleteByUserIdAndEventId(userId, eventId);
        }
        return EventMapper.toResponse(event, status, enabled);
    }

    private Set<String> remindedEventIds(String userId) {
        Set<String> ids = new HashSet<>();
        for (EventReminder reminder : reminderRepository.findByUserId(userId)) {
            ids.add(reminder.getEventId());
        }
        return ids;
    }
}
