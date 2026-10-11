package com.gamerift.feature.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Real numbers for the public landing page (no login needed). viewersNow is the sum of the live streams' viewer counts. */
@Getter
@AllArgsConstructor
public class PublicStatsResponse {

    private long users;
    private long liveStreams;
    private long channels;
    private long viewersNow;
}
