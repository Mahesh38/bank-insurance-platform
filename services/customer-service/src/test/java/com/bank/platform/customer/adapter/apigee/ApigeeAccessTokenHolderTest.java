package com.bank.platform.customer.adapter.apigee;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("FUNC-031")
class ApigeeAccessTokenHolderTest {

    @Test
    void concurrentExpiredRefreshFetchesOnce() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
        ApigeeTokenClient client = () -> {
            fetches.incrementAndGet();
            return new ApigeeAccessTokenHolder.IssuedToken(
                "token-" + fetches.get(), clock.instant().plus(Duration.ofMinutes(5)));
        };
        ApigeeAccessTokenHolder holder = new ApigeeAccessTokenHolder(client, clock);

        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    start.await();
                    holder.currentAccessToken();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            workers[i].start();
        }
        start.countDown();
        done.await();
        for (Thread worker : workers) {
            worker.join();
        }

        assertThat(holder.currentAccessToken()).isEqualTo("token-1");
        assertThat(fetches.get()).isEqualTo(1);
    }

    @Test
    void reusableTokenDoesNotRefetch() {
        AtomicInteger fetches = new AtomicInteger();
        Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
        ApigeeAccessTokenHolder holder = new ApigeeAccessTokenHolder(
            () -> {
                fetches.incrementAndGet();
                return new ApigeeAccessTokenHolder.IssuedToken(
                    "cached", clock.instant().plus(Duration.ofMinutes(5)));
            },
            clock);

        holder.currentAccessToken();
        holder.currentAccessToken();
        assertThat(fetches.get()).isEqualTo(1);
    }
}
