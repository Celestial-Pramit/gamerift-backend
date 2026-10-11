package com.gamerift.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;

import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring): shows what a token must and must not do. */
class JwtUtilTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    private User user() {
        User u = new User();
        u.setId("user-1");
        u.setEmail("a@b.com");
        u.setRole(Role.VIEWER);
        return u;
    }

    @Test
    void validToken_returnsTheUserId() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        String token = jwtUtil.generateToken(user());
        assertNotNull(token);
        assertEquals("user-1", jwtUtil.validateAndGetUserId(token));
    }

    @Test
    void tamperedToken_isRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        String token = jwtUtil.generateToken(user());
        String broken = token.substring(0, token.length() - 3) + "abc";
        assertNull(jwtUtil.validateAndGetUserId(broken));
    }

    @Test
    void expiredToken_isRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1000);
        String token = jwtUtil.generateToken(user());
        assertNull(jwtUtil.validateAndGetUserId(token));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        JwtUtil other = new JwtUtil("ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJ", 60000);
        String token = other.generateToken(user());
        assertNull(new JwtUtil(SECRET, 60000).validateAndGetUserId(token));
    }

    @Test
    void garbageText_isRejected() {
        assertNull(new JwtUtil(SECRET, 60000).validateAndGetUserId("not-a-token"));
    }

    @Test
    void shortSecret_isNotAllowed() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("short", 1000));
    }
}
