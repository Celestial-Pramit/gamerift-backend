package com.gamerift.feature.event.service;

import java.util.List;

import com.gamerift.feature.event.dto.EventRequest;
import com.gamerift.feature.event.dto.EventResponse;

/** Admin management of events (create, update, delete). Reading and "Remind me" stay in IEventService. */
public interface IAdminEventService {

    /** All events, newest start time first. */
    List<EventResponse> getAll();

    /** Creates an event. Throws 400 if raised is more than the goal. */
    EventResponse create(EventRequest request);

    /** Updates an event. Throws 404 if missing. */
    EventResponse update(String id, EventRequest request);

    /** Deletes an event and all reminders for it. Throws 404 if missing. */
    void delete(String id);
}
