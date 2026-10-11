package com.gamerift.feature.event.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.gamerift.feature.event.dto.EventResponse;
import com.gamerift.feature.event.model.GameEvent;

/** Copies a GameEvent into an EventResponse and decides the status. "now" is a parameter so tests can control time. */
public final class EventMapper {

    // We assume every event lasts 3 hours: after that it is "past".
    static final long EVENT_HOURS = 3;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Dhaka"));

    private EventMapper() {
    }

    /** "upcoming" before the start, "live" during the 3 hours after the start, then "past". */
    static String statusOf(GameEvent event, Instant now) {
        if (now.isBefore(event.getStartsAt())) {
            return "upcoming";
        }
        if (now.isBefore(event.getStartsAt().plus(EVENT_HOURS, ChronoUnit.HOURS))) {
            return "live";
        }
        return "past";
    }

    static EventResponse toResponse(GameEvent event, String status, boolean reminded) {
        EventResponse r = new EventResponse();
        r.setId(event.getId());
        r.setTitle(event.getTitle());
        r.setCategory(event.getCategory());
        r.setStartsAt(event.getStartsAt());
        r.setTime(TIME.format(event.getStartsAt()) + " BDT");
        r.setNote(event.getNote());
        r.setBadge(event.getBadge() == null ? "" : event.getBadge());
        r.setGoal(event.getGoal());
        r.setRaised(event.getRaised());
        r.setStatus(status);
        r.setReminded(reminded);
        return r;
    }
}
