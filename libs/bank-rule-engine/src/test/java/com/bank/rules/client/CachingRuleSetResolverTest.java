package com.bank.rules.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CachingRuleSetResolverTest {

  private static final RuleSet ROUTING =
      RuleSetSeeds.all().stream()
          .map(s -> RuleSetParser.parse(s.json()))
          .filter(r -> r.ruleSetId().equals("HUB.ROUTING"))
          .findFirst()
          .orElseThrow();
  private static final OffsetDateTime IN_FORCE = OffsetDateTime.parse("2026-12-01T00:00:00Z");

  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicLong clock = new AtomicLong();
  private boolean failing;

  private final RuleSetResolver delegate =
      new RuleSetResolver() {
        @Override
        public RuleSet resolve(RuleSetQuery query) {
          calls.incrementAndGet();
          if (failing) {
            throw new RuleSetUnresolvableException("down");
          }
          return ROUTING;
        }

        @Override
        public RuleSet version(String ruleSetId, String version) {
          calls.incrementAndGet();
          return ROUTING;
        }
      };

  private final CachingRuleSetResolver cache =
      new CachingRuleSetResolver(delegate, Duration.ofSeconds(60), clock::get);

  private static RuleSetQuery query(OffsetDateTime asOf) {
    return new RuleSetQuery("HUB_ROUTING", null, null, asOf);
  }

  @Test
  void servesFromCacheWithinTtlAndReloadsAfter() {
    cache.resolve(query(IN_FORCE));
    cache.resolve(query(IN_FORCE));
    assertThat(calls).hasValue(1);
    clock.set(Duration.ofSeconds(61).toNanos());
    cache.resolve(query(IN_FORCE));
    assertThat(calls).hasValue(2);
  }

  @Test
  void asOfOutsideTheCachedWindowGoesToTheService() {
    cache.resolve(query(IN_FORCE));
    cache.resolve(query(OffsetDateTime.parse("2026-01-01T00:00:00Z")));
    assertThat(calls).hasValue(2);
  }

  @Test
  void invalidationOnPublishDropsTheEntry() {
    cache.resolve(query(IN_FORCE));
    cache.invalidate("OTHER.SET");
    cache.resolve(query(IN_FORCE));
    assertThat(calls).hasValue(1);
    cache.invalidate("HUB.ROUTING");
    cache.resolve(query(IN_FORCE));
    assertThat(calls).hasValue(2);
  }

  @Test
  void failureIsNeverServedFromAStaleEntry() {
    cache.resolve(query(IN_FORCE));
    clock.set(Duration.ofMinutes(5).toNanos());
    failing = true;
    assertThatThrownBy(() -> cache.resolve(query(IN_FORCE)))
        .isInstanceOf(RuleSetUnresolvableException.class);
  }

  @Test
  void exactVersionsAreCachedUntilInvalidateAll() {
    cache.version("HUB.ROUTING", "1.0.0");
    cache.version("HUB.ROUTING", "1.0.0");
    assertThat(calls).hasValue(1);
    cache.invalidateAll();
    cache.version("HUB.ROUTING", "1.0.0");
    assertThat(calls).hasValue(2);
    assertThat(
            new CachingRuleSetResolver(delegate, CachingRuleSetResolver.DEFAULT_TTL)
                .resolve(query(IN_FORCE)))
        .isSameAs(ROUTING);
  }
}
