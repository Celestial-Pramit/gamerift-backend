package com.gamerift.feature.event.api;

import java.util.List;

import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.dto.ReminderRequest;
import com.gamerift.feature.event.service.IEventService;
import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for the Events page. All URLs need a login.
 * Request flow of "Remind me": Browser -> EventController -> IEventService (EventServiceImpl)
 *   -> GameEventRepository + EventReminderRepository -> MongoDB -> EventResponse JSON.
 */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Upcoming, live and past events with Remind me")
public class EventController {

    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Events for a tab (all, upcoming, live, past, reminders) and an optional category")
    public List<EventResponse> getAll(@RequestParam(required = false) String tab,
                                      @RequestParam(required = false) String category,
                                      @AuthenticationPrincipal AuthUser authUser) {
        return eventService.getAll(authUser.getId(), tab, category);
    }

    @PutMapping("/{id}/remind")
    @Operation(summary = "Switch Remind me on or off")
    public EventResponse remind(@PathVariable String id, @RequestBody ReminderRequest request,
                                @AuthenticationPrincipal AuthUser authUser) {
        return eventService.setReminder(authUser.getId(), id, request.isEnabled());
    }
}
