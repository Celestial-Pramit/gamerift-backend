package com.gamerift.feature.auth.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A password-reset link token (collection "password_reset_tokens").
 * The real token is only in the e-mail; here we store its SHA-256 hash. Valid 15 minutes, single use.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
