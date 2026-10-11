package com.gamerift.feature.stream.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One stream card (collection "streams").
 * (The class is called LiveStream so it is not confused with java.util.stream.Stream.)
 *
 * The video is NOT a real live stream: it is a looping YouTube/MP4 clip. "baseViewers" is the
 * starting number for the fake viewer counter that the frontend drifts around.
 * The compound index speeds up the Discover page query "category + phase".
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "streams")
@CompoundIndex(name = "category_phase", def = "{'categorySlug': 1, 'phase': 1}")
public class LiveStream {

    @Id
    private String id;

    private String title;
    private String description;
    private String categorySlug;
    private String channelId;
    // Copy of the channel name, so list pages do not need a second query
    private String channelName;
    private VideoSource videoSource = VideoSource.YOUTUBE;
    private String videoUrl;
    private String thumbnailUrl;
    private StreamPhase phase = StreamPhase.SOON;
    // false = draft (visible only to admins)
    private boolean published = true;
    private boolean featured;
    private int baseViewers;
    private Instant startedAt;
    private Instant scheduledAt;
    private List<String> tags = new ArrayList<>();
    private int likesCount;
    // Recalculated by a scheduled job in a later phase
    private double trendScore;
    private Instant createdAt = Instant.now();
}
