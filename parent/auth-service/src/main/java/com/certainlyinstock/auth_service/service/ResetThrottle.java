package com.certainlyinstock.auth_service.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Allows one reset email per address per cooldown window, so the endpoint cannot be used to
 * flood someone's inbox. In-memory and per instance; per-IP limits belong at the gateway.
 */
@Component
public class ResetThrottle {

    private static final int PURGE_THRESHOLD = 1000;

    private final Duration cooldown;
    private final Clock clock;
    private final Map<String, Instant> lastAccepted = new HashMap<>();

    @Autowired
    public ResetThrottle() {
        this(Duration.ofSeconds(60), Clock.systemUTC());
    }

    public ResetThrottle(Duration cooldown, Clock clock) {
        this.cooldown = cooldown;
        this.clock = clock;
    }

    /** True if a reset may be sent for this address now (and records it); false if too soon. */
    public synchronized boolean tryAcquire(String email) {
        Instant now = clock.instant();
        if (lastAccepted.size() >= PURGE_THRESHOLD) {
            lastAccepted.values().removeIf(t -> t.plus(cooldown).isBefore(now));
        }
        Instant last = lastAccepted.get(email);
        if (last != null && now.isBefore(last.plus(cooldown))) {
            return false;
        }
        lastAccepted.put(email, now);
        return true;
    }
}
