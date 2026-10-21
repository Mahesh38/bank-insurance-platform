package com.bank.platform.lead.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-030")
class UlidTest {

  @Test
  void mintsCrockford26AndSortsByTime() {
    Clock early = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    Clock later = Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC);

    String first = Ulid.mint(early);
    String second = Ulid.mint(later);

    assertThat(first).hasSize(26);
    assertThat(Ulid.isValid(first)).isTrue();
    assertThat(second).isGreaterThan(first);
    assertThat(first).doesNotContain("I", "L", "O", "U");
  }

  @Test
  void rejectsWrongLengthAndForbiddenLetters() {
    assertThat(Ulid.isValid("short")).isFalse();
    assertThat(Ulid.isValid("01JQX4K7R8M2N3P4Q5S6T7ILOU")).isFalse();
    assertThat(Ulid.isValid(null)).isFalse();
  }
}
