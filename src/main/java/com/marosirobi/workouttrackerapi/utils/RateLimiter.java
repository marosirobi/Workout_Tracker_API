package com.marosirobi.workouttrackerapi.utils;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {

    private record Window(Instant resetAt, int count) { }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        Window current = windows.compute(key, (k, existing) ->
                existing == null || !now.isBefore(existing.resetAt())
                        ? new Window(now.plus(window), 1)
                        : new Window(existing.resetAt(), existing.count() + 1));
        return current.count() <= limit;
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    void removeExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(window -> !now.isBefore(window.resetAt()));
    }
}
