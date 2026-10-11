package com.gamerift.feature.channel.repository;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.model.Follow;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Derived queries: the method names are translated into MongoDB queries by Spring Data. */
public interface FollowRepository extends MongoRepository<Follow, String> {

    List<Follow> findByUserId(String userId);

    List<Follow> findByChannelId(String channelId);

    /** Returns only the userIds of followers for the given channel. */
    List<String> findUserIdsByChannelId(String channelId);

    Optional<Follow> findByUserIdAndChannelId(String userId, String channelId);

    boolean existsByUserIdAndChannelId(String userId, String channelId);
}
