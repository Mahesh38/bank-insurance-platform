package com.bank.platform.consent.config;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-process limit for the public consent link. A 256-bit token cannot be guessed, and this stops
 * one source from spraying tokens or holding the page open. It is not a network DDoS control: that
 * sits on the edge WAF in front of every replica. The client address is the socket peer, not a
 * caller-supplied forwarded header.
 */
public final class PublicLinkRateLimiter {

  private final int perMinute;
  private final int perSecond;
  private final ConcurrentHashMap<String, Deque<Long>> byIp = new ConcurrentHashMap<>();
  private final AtomicLong windowSecond = new AtomicLong(-1);
  private final AtomicInteger windowCount = new AtomicInteger();

  public PublicLinkRateLimiter(int perMinute, int perSecond) {
    this.perMinute = perMinute;
    this.perSecond = perSecond;
  }

  public boolean allow(String ip) {
    if (!allowGlobal()) {
      return false;
    }
    String key = ip == null || ip.isBlank() ? "unknown" : ip;
    Deque<Long> hits = byIp.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    long now = System.currentTimeMillis();
    synchronized (hits) {
      while (!hits.isEmpty() && now - hits.peekFirst() > 60_000L) {
        hits.removeFirst();
      }
      if (hits.size() >= perMinute) {
        return false;
      }
      hits.addLast(now);
      return true;
    }
  }

  public void reset() {
    byIp.clear();
    windowCount.set(0);
    windowSecond.set(-1);
  }

  private boolean allowGlobal() {
    long second = System.currentTimeMillis() / 1000L;
    long seen = windowSecond.get();
    if (seen != second && windowSecond.compareAndSet(seen, second)) {
      windowCount.set(0);
    }
    return windowCount.incrementAndGet() <= perSecond;
  }
}
