package com.bank.common.apigee;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class ApigeeAccessTokenHolderTest {

  @Test
  void concurrentExpiredRefreshFetchesOnce() throws Exception {
    AtomicInteger fetches = new AtomicInteger();
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
    ApigeeTokenClient client =
        () -> {
          fetches.incrementAndGet();
          return new IssuedToken(
              "token-" + fetches.get(), clock.instant().plus(Duration.ofMinutes(5)));
        };
    ApigeeAccessTokenHolder holder = new ApigeeAccessTokenHolder(client, clock);

    int threads = 16;
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    Thread[] workers = new Thread[threads];
    for (int i = 0; i < threads; i++) {
      workers[i] =
          new Thread(
              () -> {
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
    ApigeeAccessTokenHolder holder =
        new ApigeeAccessTokenHolder(
            () -> {
              fetches.incrementAndGet();
              return new IssuedToken("cached", clock.instant().plus(Duration.ofMinutes(5)));
            },
            clock);

    holder.currentAccessToken();
    holder.currentAccessToken();
    assertThat(fetches.get()).isEqualTo(1);
  }

  @Test
  void invalidateForcesRemint() {
    AtomicInteger fetches = new AtomicInteger();
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
    ApigeeAccessTokenHolder holder =
        new ApigeeAccessTokenHolder(
            () -> {
              int n = fetches.incrementAndGet();
              return new IssuedToken("token-" + n, clock.instant().plus(Duration.ofMinutes(5)));
            },
            clock);

    assertThat(holder.currentAccessToken()).isEqualTo("token-1");
    holder.invalidate();
    assertThat(holder.currentAccessToken()).isEqualTo("token-2");
    assertThat(fetches.get()).isEqualTo(2);
  }

  @Test
  void expiredTokenIsRefetched() {
    AtomicInteger fetches = new AtomicInteger();
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-05T12:00:00Z"));
    Clock clock = mutableClock(now);
    ApigeeAccessTokenHolder holder =
        new ApigeeAccessTokenHolder(
            () -> {
              int n = fetches.incrementAndGet();
              return new IssuedToken("token-" + n, now.get().plus(Duration.ofSeconds(40)));
            },
            clock);

    assertThat(holder.currentAccessToken()).isEqualTo("token-1");
    now.set(now.get().plus(Duration.ofSeconds(15)));
    assertThat(holder.currentAccessToken()).isEqualTo("token-2");
    assertThat(fetches.get()).isEqualTo(2);
  }

  @Test
  void waitersReuseTokenMintedUnderLock() throws Exception {
    AtomicInteger fetches = new AtomicInteger();
    CountDownLatch inFetch = new CountDownLatch(1);
    CountDownLatch releaseFetch = new CountDownLatch(1);
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);
    ApigeeTokenClient client =
        () -> {
          fetches.incrementAndGet();
          inFetch.countDown();
          try {
            releaseFetch.await();
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
          }
          return new IssuedToken("shared", clock.instant().plus(Duration.ofMinutes(5)));
        };
    ApigeeAccessTokenHolder holder = new ApigeeAccessTokenHolder(client, clock);

    Thread first = new Thread(holder::currentAccessToken, "apigee-holder-first");
    first.start();
    inFetch.await();
    Thread second = new Thread(holder::currentAccessToken, "apigee-holder-second");
    second.start();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (second.getState() != Thread.State.WAITING
        && second.getState() != Thread.State.TIMED_WAITING
        && second.getState() != Thread.State.BLOCKED) {
      if (System.nanoTime() > deadline || second.getState() == Thread.State.TERMINATED) {
        throw new AssertionError("second thread did not block on the holder lock");
      }
      Thread.onSpinWait();
    }
    releaseFetch.countDown();
    first.join();
    second.join();

    assertThat(holder.currentAccessToken()).isEqualTo("shared");
    assertThat(fetches.get()).isEqualTo(1);
  }

  private static Clock mutableClock(AtomicReference<Instant> now) {
    return new Clock() {
      @Override
      public ZoneId getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return now.get();
      }
    };
  }
}
