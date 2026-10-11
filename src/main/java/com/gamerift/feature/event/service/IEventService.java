package com.gamerift.feature.event.service;

import java.util.List;

import com.gamerift.feature.event.dto.EventResponse;

/** What the event feature can do (read + "Remind me"). Admin create/update/delete comes in a later part. */
public interface IEventService {

    /**
     * The Events page. tab = all | upcoming | live | past | reminders (default all).
     * category is optional (case does not matter).
     */
    List<EventResponse> getAll(String userId, String tab, String category);

    /** Switch "Remind me" on or off for this user. Events that are over cannot get a new reminder. */
    EventResponse setReminder(String userId, String eventId, boolean enabled);
}
