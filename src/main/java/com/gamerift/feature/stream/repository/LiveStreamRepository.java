package com.gamerift.feature.stream.repository;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Derived queries: Spring reads the method name and builds the MongoDB query for us. */
public interface LiveStreamRepository extends MongoRepository<LiveStream, String> {

    // published = true AND phase = ?
    List<LiveStream> findByPublishedTrueAndPhase(StreamPhase phase);

    List<LiveStream> findByCategorySlug(String categorySlug);

    // true if at least one stream uses this category (used before changing or deleting a category)
    boolean existsByCategorySlug(String categorySlug);

    // used to keep only ONE featured (hero) stream
    List<LiveStream> findByFeaturedTrue();

    // the most watched featured stream (the hero card on the dashboard)
    Optional<LiveStream> findFirstByPublishedTrueAndFeaturedTrueOrderByBaseViewersDesc();

    // "Top8 ... OrderByTrendScoreDesc": the 8 best trend scores of one phase
    List<LiveStream> findTop8ByPublishedTrueAndPhaseOrderByTrendScoreDesc(StreamPhase phase);

    // candidates for "related streams": same category, most watched first
    List<LiveStream> findTop7ByCategorySlugAndPublishedTrueOrderByBaseViewersDesc(String categorySlug);

    // true if any stream belongs to this channel (used to block deleting a channel that still has streams)
    boolean existsByChannelId(String channelId);
}
