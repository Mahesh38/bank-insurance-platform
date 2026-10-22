package com.bank.rules.client;

import com.bank.rules.engine.RuleSet;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * L1 cache in front of a {@link RuleSetResolver} (ADR-024 section 5 point 4). An entry serves a
 * query only while it is younger than the TTL (default 60 s — the bound on staleness when a {@code
 * RuleSetPublished} event is lost) and while the query's {@code asOf} falls inside the cached
 * version's effective window. Exact versions are immutable once published and are kept until {@link
 * #invalidateAll()}.
 *
 * <p>A failed resolution is never cached and never answered from a stale entry: fail closed.
 */
public final class CachingRuleSetResolver implements RuleSetResolver {

  public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

  private final RuleSetResolver delegate;
  private final long ttlNanos;
  private final LongSupplier nanoTime;
  private final Map<String, Entry> byScope = new ConcurrentHashMap<>();
  private final Map<String, RuleSet> byVersion = new ConcurrentHashMap<>();

  private record Entry(RuleSet ruleSet, long loadedAt) {}

  public CachingRuleSetResolver(RuleSetResolver delegate, Duration ttl) {
    this(delegate, ttl, System::nanoTime);
  }

  /** {@code nanoTime} is injectable so tests control expiry without sleeping. */
  public CachingRuleSetResolver(RuleSetResolver delegate, Duration ttl, LongSupplier nanoTime) {
    this.delegate = delegate;
    this.ttlNanos = ttl.toNanos();
    this.nanoTime = nanoTime;
  }

  @Override
  public RuleSet resolve(RuleSetQuery query) {
    long now = nanoTime.getAsLong();
    Entry e = byScope.get(query.scopeKey());
    if (e != null && now - e.loadedAt() < ttlNanos && inForce(e.ruleSet(), query.asOf())) {
      return e.ruleSet();
    }
    RuleSet fresh = delegate.resolve(query);
    byScope.put(query.scopeKey(), new Entry(fresh, now));
    return fresh;
  }

  @Override
  public RuleSet version(String ruleSetId, String version) {
    return byVersion.computeIfAbsent(
        ruleSetId + "@" + version, k -> delegate.version(ruleSetId, version));
  }

  /** Called on {@code RuleSetPublished}: drop every scope entry of that rule set. */
  public void invalidate(String ruleSetId) {
    byScope.values().removeIf(e -> e.ruleSet().ruleSetId().equals(ruleSetId));
  }

  public void invalidateAll() {
    byScope.clear();
    byVersion.clear();
  }

  private static boolean inForce(RuleSet rs, OffsetDateTime asOf) {
    return !asOf.isBefore(rs.effectiveFrom())
        && (rs.effectiveTo() == null || asOf.isBefore(rs.effectiveTo()));
  }
}
