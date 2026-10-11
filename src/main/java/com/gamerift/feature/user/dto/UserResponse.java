package com.gamerift.feature.user.dto;

import java.time.Instant;

/**
 * What we tell the frontend about a user. NEVER includes the password hash.
 * The getter names decide the JSON field names (getIsPro -> "isPro", getTwoFactor -> "twoFactor"),
 * which is what the React app already expects.
 */
public class UserResponse {

    private final String id;
    private final String username;
    private final String email;
    private final String role;
    private final String country;
    private final String avatarUrl;
    private final String bio;
    private final boolean emailVerified;
    private final boolean twoFactor;
    private final boolean pro;
    private final Instant proUntil;
    private final Instant createdAt;

    public UserResponse(String id, String username, String email, String role, String country, String avatarUrl,
                        String bio, boolean emailVerified, boolean twoFactor, boolean pro, Instant proUntil, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.country = country;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.emailVerified = emailVerified;
        this.twoFactor = twoFactor;
        this.pro = pro;
        this.proUntil = proUntil;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getCountry() { return country; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean getTwoFactor() { return twoFactor; }
    public boolean getIsPro() { return pro; }
    public Instant getProUntil() { return proUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
