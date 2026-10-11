package com.gamerift.shared.util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gamerift.shared.exception.TooManyRequestsException;

import org.springframework.stereotype.Component;

/**
 * A very small in-memory rate limiter ("sliding window").
 * For every key (for example "login:a@b.com") we remember the times of the last requests.
 * If there were already "max" requests inside the window, the next one is refused.
 *
 * Simple on purpose: it lives in memory, so it resets when the app restarts and it is per server instance.
 * That is enough to stop password guessing and e-mail spamming in this project.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Returns true if the request is allowed (and records it), false if the limit is reached. */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            // forget requests that are older than the window
            while (!times.isEmpty() && times.peekFirst() < windowStart) {
                times.pollFirst();
            }
            if (times.size() >= maxRequests) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    /** Same as tryAcquire, but throws HTTP 429 when the limit is reached. */
    public void check(String key, int maxRequests, Duration window, String message) {
        if (!tryAcquire(key, maxRequests, window)) {
            throw new TooManyRequestsException(message);
        }
    }
}
