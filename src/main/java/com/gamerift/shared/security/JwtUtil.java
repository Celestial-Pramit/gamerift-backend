package com.gamerift.shared.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

import com.gamerift.feature.user.model.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and checks JWT login tokens.
 *
 * A JWT has 3 parts: header.payload.signature (Base64). The payload holds "claims" such as the
 * user id (subject), role and expiry time. The signature is an HMAC-SHA256 made with OUR secret,
 * so nobody can change the payload without us noticing. We never store tokens on the server (stateless).
 */
@Component
public class JwtUtil {

    private final byte[] secret;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // HS256 needs at least 256 bits = 32 bytes
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters. Set it in the .env file.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    /** Builds and signs a token for this user. */
    public String generateToken(User user) {
        Date now = new Date();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + expirationMs))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the token", e);
        }
    }

    /**
     * Checks the signature and the expiry date.
     * Returns the user id when the token is good, or null when it is broken, tampered or expired.
     */
    public String validateAndGetUserId(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                return null;
            }
            Date expires = jwt.getJWTClaimsSet().getExpirationTime();
            if (expires == null || expires.before(new Date())) {
                return null;
            }
            return jwt.getJWTClaimsSet().getSubject();
        } catch (ParseException | JOSEException e) {
            return null;
        }
    }
}
