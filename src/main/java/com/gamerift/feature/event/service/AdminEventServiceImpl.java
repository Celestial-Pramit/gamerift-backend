package com.gamerift.feature.event.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.event.dto.EventRequest;
import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.EventReminderRepository;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for managing events (layer: service).
 * Called by: AdminEventController. Calls: GameEventRepository, EventReminderRepository.
 * Rules: raised cannot be more than the goal (when a goal is set); deleting an event also deletes its reminders.
 * It lives in the same package as EventMapper so it can reuse the status rule (upcoming / live / past).
 */
@Service
public class AdminEventServiceImpl implements IAdminEventService {

    private final GameEventRepository eventRepository;
    private final EventReminderRepository reminderRepository;
    private final INotificationService notificationService;

    public AdminEventServiceImpl(GameEventRepository eventRepository,
                                 EventReminderRepository reminderRepository,
                                 INotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<EventResponse> getAll() {
        Instant now = Instant.now();
        List<EventResponse> result = new ArrayList<>();
        for (GameEvent event : eventRepository.findAll()) {
            result.add(EventMapper.toResponse(event, EventMapper.statusOf(event, now), false));
        }
        // newest start time first
        result.sort((a, b) -> b.getStartsAt().compareTo(a.getStartsAt()));
        return result;
    }

    @Override
    public EventResponse create(EventRequest request) {
        // 1. business rule
        checkMoney(request);
        // 2. build and save
        GameEvent event = new GameEvent();
        copyInto(event, request);
        GameEvent saved = eventRepository.save(event);
        EventResponse response = toResponse(saved);
        // 3. notify about the new event (followers or notifyEvents users)
        try {
            notificationService.notifyEventCreated(saved.getId(), saved.getTitle(), null);
        } catch (Exception e) {
            // Notification failure must never fail the admin action
        }
        return response;
    }

    @Override
    public EventResponse update(String id, EventRequest request) {
        // 1. the event must exist
        GameEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        // 2. business rule
        checkMoney(request);
        // 3. overwrite and save
        copyInto(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public void delete(String id) {
        // 1. the event must exist
        if (!eventRepository.existsById(id)) {
            throw new NotFoundException("Event not found");
        }
        // 2. remove the reminders first so nothing points to a missing event
        reminderRepository.deleteByEventId(id);
        // 3. delete the event
        eventRepository.deleteById(id);
    }

    private void checkMoney(EventRequest request) {
        if (request.getGoal() > 0 && request.getRaised() > request.getGoal()) {
            throw new BadRequestException("Raised cannot be more than the goal");
        }
    }

    private void copyInto(GameEvent event, EventRequest request) {
        event.setTitle(request.getTitle().trim());
        event.setCategory(request.getCategory().trim().toUpperCase());
        event.setStartsAt(request.getStartsAt());
        event.setNote(request.getNote());
        event.setBadge(request.getBadge() == null ? "" : request.getBadge());
        event.setGoal(request.getGoal());
        event.setRaised(request.getRaised());
    }

    private EventResponse toResponse(GameEvent event) {
        return EventMapper.toResponse(event, EventMapper.statusOf(event, Instant.now()), false);
    }
}
