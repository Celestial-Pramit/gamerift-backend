package com.gamerift.feature.event.api;

import java.util.List;

import com.gamerift.feature.event.dto.EventRequest;
import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.service.IAdminEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only management of events (admin panel "Events" screen).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here.
 * Request flow of "create": Browser -> AdminEventController (@Valid) -> IAdminEventService
 *   -> GameEventRepository -> MongoDB -> EventResponse JSON (201).
 * A later phase will notify users when a new event is created.
 */
@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Events", description = "Create, update and delete events (ADMIN only)")
public class AdminEventController {

    private final IAdminEventService adminEventService;

    public AdminEventController(IAdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    @Operation(summary = "All events, newest first")
    public List<EventResponse> getAll() {
        return adminEventService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event")
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return adminEventService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event")
    public EventResponse update(@PathVariable String id, @Valid @RequestBody EventRequest request) {
        return adminEventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an event and its reminders")
    public void delete(@PathVariable String id) {
        adminEventService.delete(id);
    }
}
