package com.gamerift.feature.event.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An event for the Events page and the dashboard.
 * status is "upcoming", "live" or "past" (calculated from the start time).
 * reminded tells whether the LOGGED-IN user switched "Remind me" on.
 */
@Getter
@Setter
@NoArgsConstructor
public class EventResponse {

    private String id;
    private String title;
    private String category;
    private Instant startsAt;
    // start time in Bangladesh time, for example "20:30 BDT"
    private String time;
    private String note;
    // "", "soon", "fund" or "pro"
    private String badge;
    private long goal;
    private long raised;
    private String status;
    private boolean reminded;
}
