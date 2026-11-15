package com.marosirobi.workouttrackerapi.utils;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-13T10:00:00Z");

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter rateLimiter = new RateLimiter(clock);

    @Test
    void allowsUpToLimit_thenBlocks() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("key", 3, WINDOW)).isTrue();
        }
        assertThat(rateLimiter.tryAcquire("key", 3, WINDOW)).isFalse();
    }

    @Test
    void resetsAfterWindow() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.tryAcquire("key", 3, WINDOW);
        }

        clock.advance(WINDOW);

        assertThat(rateLimiter.tryAcquire("key", 3, WINDOW)).isTrue();
    }

    @Test
    void keysAreIndependent() {
        rateLimiter.tryAcquire("a", 1, WINDOW);

        assertThat(rateLimiter.tryAcquire("a", 1, WINDOW)).isFalse();
        assertThat(rateLimiter.tryAcquire("b", 1, WINDOW)).isTrue();
    }
}
