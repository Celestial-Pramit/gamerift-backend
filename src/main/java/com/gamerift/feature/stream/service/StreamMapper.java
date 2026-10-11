package com.gamerift.feature.stream.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

/** Copies a LiveStream (model) into a StreamResponse (DTO). "now" is a parameter so tests can use a fixed time. */
public final class StreamMapper {

    private StreamMapper() {
    }

    public static StreamResponse toResponse(LiveStream s, boolean channelPro, Instant now) {
        StreamResponse r = new StreamResponse();
        r.setId(s.getId());
        r.setTitle(s.getTitle());
        r.setDescription(s.getDescription());
        r.setChannelId(s.getChannelId());
        r.setChannelName(s.getChannelName());
        r.setPro(channelPro);
        r.setCategorySlug(s.getCategorySlug());
        r.setLabel(s.getCategorySlug() == null ? "" : s.getCategorySlug().toUpperCase());
        r.setVideoSource(s.getVideoSource().name());
        r.setVideoUrl(s.getVideoUrl() == null ? "" : s.getVideoUrl());
        r.setThumbnailUrl(s.getThumbnailUrl() == null ? "" : s.getThumbnailUrl());
        r.setStatus(s.getPhase().name().toLowerCase());
        // only a LIVE stream has viewers
        r.setViewers(s.getPhase() == StreamPhase.LIVE ? s.getBaseViewers() : 0);
        r.setBaseViewers(s.getBaseViewers());
        r.setMinutes(minutes(s, now));
        r.setTags(new ArrayList<>(s.getTags()));
        r.setFeatured(s.isFeatured());
        r.setPublished(s.isPublished());
        r.setLikesCount(s.getLikesCount());
        r.setTrendScore(s.getTrendScore());
        r.setStartedAt(s.getStartedAt());
        r.setScheduledAt(s.getScheduledAt());
        return r;
    }

    private static long minutes(LiveStream s, Instant now) {
        if (s.getPhase() == StreamPhase.LIVE && s.getStartedAt() != null) {
            return Math.max(0, Duration.between(s.getStartedAt(), now).toMinutes());
        }
        if (s.getPhase() == StreamPhase.SOON && s.getScheduledAt() != null) {
            return Math.max(0, Duration.between(now, s.getScheduledAt()).toMinutes());
        }
        return 0;
    }
}
