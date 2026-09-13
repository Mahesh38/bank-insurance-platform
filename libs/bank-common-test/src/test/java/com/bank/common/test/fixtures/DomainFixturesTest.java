package com.bank.common.test.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DomainFixturesTest {

  @Test
  void identifiers_arePrefixedAndUnique() {
    assertThat(DomainFixtures.journeyId()).hasSize(36);
    assertThat(DomainFixtures.journeyId()).isNotEqualTo(DomainFixtures.journeyId());
    assertThat(DomainFixtures.idempotencyKey("create")).startsWith("create-");
    assertThat(DomainFixtures.actorId()).startsWith("actor-");
    assertThat(DomainFixtures.jobId()).hasSize(36);
  }

  @Test
  void idempotencyKey_truncatesWhenPrefixPlusUuidExceeds64() {
    String longPrefix = "p".repeat(64);
    String key = DomainFixtures.idempotencyKey(longPrefix);
    assertThat(key).hasSize(36).doesNotStartWith(longPrefix);
  }
}
