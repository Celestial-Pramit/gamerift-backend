package com.gamerift.feature.match.repository;

import com.gamerift.feature.match.model.MatchCard;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchCardRepository extends MongoRepository<MatchCard, String> {
}
