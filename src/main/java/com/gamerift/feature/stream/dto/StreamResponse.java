package com.gamerift.feature.stream.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A stream card as the React app needs it (same field names as the frontend mock data).
 * status is "live", "soon" or "ended". minutes = minutes since it started (live) or until it starts (soon).
 */
@Getter
@Setter
@NoArgsConstructor
public class StreamResponse {

    private String id;
    private String title;
    private String description;
    private String channelId;
    private String channelName;
    // true when the channel is a Pro channel (gold PRO tag)
    private boolean pro;
    private String categorySlug;
    // Category name in capitals for the tag, for example "ESPORTS"
    private String label;
    private String videoSource;
    private String videoUrl;
    private String thumbnailUrl;
    private String status;
    private int viewers;
    private long minutes;
    private List<String> tags = new ArrayList<>();
    private boolean featured;
    // false = draft (only admins see it)
    private boolean published;
    private int likesCount;
    // the starting viewer number (the admin form needs it even when the stream is not live)
    private int baseViewers;
    private double trendScore;
    private Instant startedAt;
    private Instant scheduledAt;
}
