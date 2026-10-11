package com.gamerift.feature.stream.repository;

import com.gamerift.feature.stream.model.StreamLike;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StreamLikeRepository extends MongoRepository<StreamLike, String> {

    boolean existsByUserIdAndStreamId(String userId, String streamId);

    // derived delete
    void deleteByUserIdAndStreamId(String userId, String streamId);
}
