package com.gamerift.feature.stream.service;

import java.time.Duration;
import java.time.Instant;

import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

/**
 * The trending formula, kept simple on purpose. Only LIVE streams can trend (others score 0).
 *
 *   score = viewers / 1000  +  likes / 500  +  freshness
 *
 * freshness gives a stream that just started up to 10 points; it falls to 0 after 24 hours.
 * The result is rounded to one decimal. "now" is a parameter so tests can use a fixed time.
 */
public final class TrendScoreCalculator {

    private TrendScoreCalculator() {
    }

    public static double score(LiveStream stream, Instant now) {
        if (stream.getPhase() != StreamPhase.LIVE) {
            return 0;
        }
        double viewersPart = stream.getBaseViewers() / 1000.0;
        double likesPart = stream.getLikesCount() / 500.0;

        double freshness = 0;
        if (stream.getStartedAt() != null) {
            double hoursLive = Math.max(0, Duration.between(stream.getStartedAt(), now).toMinutes() / 60.0);
            freshness = Math.max(0, 24 - hoursLive) / 24 * 10;
        }
        return Math.round((viewersPart + likesPart + freshness) * 10) / 10.0;
    }
}
