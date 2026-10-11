package com.gamerift.feature.user.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A user account = one document in the MongoDB collection "users".
 *
 * @Document  maps the class to a collection.
 * @Id        the primary key (MongoDB creates the value).
 * @Indexed(unique = true) makes MongoDB reject two users with the same email or username.
 * Lombok @Getter/@Setter/@NoArgsConstructor generate the boring Java code for us.
 * We never store the real password: only passwordHash (BCrypt).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private Role role = Role.VIEWER;
    private String country;
    private String avatarUrl;
    private String bio;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private boolean banned;
    private boolean pro;
    private Instant proUntil;
    // notification preferences (Settings > Alerts); all switched on by default
    private boolean notifyLive = true;
    private boolean notifyEvents = true;
    private boolean notifyReplies = true;
    private boolean notifyPay = true;
    private Instant createdAt = Instant.now();
}
