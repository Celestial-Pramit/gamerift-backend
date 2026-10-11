package com.gamerift.feature.auth.repository;

import java.util.Optional;

import com.gamerift.feature.auth.model.PasswordResetToken;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(String userId);
}
