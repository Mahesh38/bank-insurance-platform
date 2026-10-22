package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UlidTest {

  @Test
  void sameMillisecondIdsSortInMintOrder() {
    Clock fixed = Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC);
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      ids.add(Ulid.mint(fixed));
    }
    assertThat(ids).isSorted().doesNotHaveDuplicates().allMatch(Ulid::isValid);
  }

  @Test
  void validation() {
    assertThat(Ulid.isValid(Ulid.mint())).isTrue();
    assertThat(Ulid.isValid(null)).isFalse();
    assertThat(Ulid.isValid("short")).isFalse();
    assertThat(Ulid.isValid("0000000000000000000000000U")).isFalse();
    assertThatThrownBy(() -> Ulid.mint(Clock.fixed(Instant.ofEpochMilli(-1), ZoneOffset.UTC)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
