package com.bank.platform.customer.adapter.apigee;

import com.bank.platform.customer.domain.AccessTokenPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single-flight Apigee client-credentials holder.
 *
 * <p>Every CBS / Customer-360 call shares one access token. Concurrent callers
 * never stampede {@code /token}: an expired or missing token is refreshed under
 * a lock, and waiters reuse the same result. The BFF never holds this token
 * ({@code ADR-015}, {@code ADR-020}).
 */
public final class ApigeeAccessTokenHolder implements AccessTokenPort {

    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(30);

    private final ApigeeTokenClient client;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile CachedToken cached;

    public ApigeeAccessTokenHolder(ApigeeTokenClient client, Clock clock) {
        this.client = Objects.requireNonNull(client);
        this.clock = Objects.requireNonNull(clock);
    }

    public String currentAccessToken() {
        CachedToken current = cached;
        Instant now = clock.instant();
        if (current != null && current.usableAt(now)) {
            return current.value();
        }
        lock.lock();
        try {
            current = cached;
            now = clock.instant();
            if (current != null && current.usableAt(now)) {
                return current.value();
            }
            IssuedToken issued = client.fetchClientCredentials();
            cached = new CachedToken(issued.value(), issued.expiresAt().minus(EXPIRY_SKEW));
            return cached.value();
        } finally {
            lock.unlock();
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    private record CachedToken(String value, Instant refreshAfter) {
        boolean usableAt(Instant now) {
            return now.isBefore(refreshAfter);
        }
    }
}
