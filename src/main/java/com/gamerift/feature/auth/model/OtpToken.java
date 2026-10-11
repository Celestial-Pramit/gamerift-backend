package com.gamerift.feature.auth.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A one-time 6-digit code (collection "otp_tokens"). We store only the BCrypt HASH of the code.
 * expiresAt has a TTL index: MongoDB deletes the document by itself after that time.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "otp_tokens")
public class OtpToken {

    @Id
    private String id;

    @Indexed
    private String email;

    private OtpPurpose purpose;
    private String codeHash;
    // wrong tries so far (max 5)
    private int attempts;
    // used for the "wait before asking again" rule
    private Instant lastSentAt;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
