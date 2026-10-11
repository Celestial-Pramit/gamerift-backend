package com.gamerift.feature.auth.repository;

import java.util.Optional;

import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.feature.auth.model.OtpToken;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {

    Optional<OtpToken> findByEmailAndPurpose(String email, OtpPurpose purpose);
}
