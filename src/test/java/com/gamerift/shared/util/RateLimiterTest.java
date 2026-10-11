package com.gamerift.shared.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import com.gamerift.shared.exception.TooManyRequestsException;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsUpToTheLimitAndThenRefuses() {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("k", 3, Duration.ofMinutes(1)));
        assertTrue(limiter.tryAcquire("k", 3, Duration.ofMinutes(1)));
        assertTrue(limiter.tryAcquire("k", 3, Duration.ofMinutes(1)));
        assertFalse(limiter.tryAcquire("k", 3, Duration.ofMinutes(1)));
    }

    @Test
    void differentKeysDoNotAffectEachOther() {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("a", 1, Duration.ofMinutes(1)));
        assertTrue(limiter.tryAcquire("b", 1, Duration.ofMinutes(1)));
    }

    @Test
    void allowsAgainWhenTheWindowHasPassed() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("k", 1, Duration.ofMillis(50)));
        assertFalse(limiter.tryAcquire("k", 1, Duration.ofMillis(50)));
        Thread.sleep(80);
        assertTrue(limiter.tryAcquire("k", 1, Duration.ofMillis(50)));
    }

    @Test
    void checkThrowsTooManyRequests() {
        RateLimiter limiter = new RateLimiter();
        limiter.check("k", 1, Duration.ofMinutes(1), "x");
        assertThrows(TooManyRequestsException.class, () -> limiter.check("k", 1, Duration.ofMinutes(1), "x"));
    }
}
