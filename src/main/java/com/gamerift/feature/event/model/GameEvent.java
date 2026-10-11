package com.gamerift.feature.event.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An upcoming, live or past event (collection "events").
 * Whether it is upcoming/live/past is decided by comparing startsAt with the current time.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "events")
public class GameEvent {

    @Id
    private String id;

    private String title;
    // ESPORTS, IRL, SPEEDRUN, WORKSHOP
    private String category;
    @Indexed
    private Instant startsAt;
    private String note;
    // "", "soon", "fund" (fundraiser) or "pro" (Pro only)
    private String badge = "";
    // Fundraiser numbers in BDT (0 when not a fundraiser)
    private long goal;
    private long raised;
}
