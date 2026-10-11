package com.gamerift.feature.stream.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.model.VideoSource;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of the admin create / update stream requests (the admin "stream form").
 * Simple rules are checked here with annotations; the rules that need the database
 * (does the category exist? is the link a YouTube link?) are checked in the service.
 */
@Getter
@Setter
@NoArgsConstructor
public class StreamRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title can have at most 120 characters")
    private String title;

    @Size(max = 500, message = "Description can have at most 500 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String categorySlug;

    @NotBlank(message = "Channel is required")
    private String channelId;

    @NotNull(message = "Video source is required")
    private VideoSource videoSource;

    @Size(max = 300, message = "Video link can have at most 300 characters")
    private String videoUrl;

    // LIVE = running now, SOON = starts later (needs scheduledAt), ENDED = finished
    @NotNull(message = "Phase is required")
    private StreamPhase phase;

    private Instant scheduledAt;

    @Min(value = 0, message = "Base viewers cannot be negative")
    @Max(value = 5_000_000, message = "Base viewers is too large")
    private int baseViewers;

    private boolean published = true;
    private boolean featured;

    @Size(max = 8, message = "At most 8 tags")
    private List<@Size(max = 20, message = "A tag can have at most 20 characters") String> tags = new ArrayList<>();
}
