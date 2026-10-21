package com.bank.common.apigee;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class IssuedTokenTest {

  @Test
  void blankValueIsRejected() {
    assertThatThrownBy(() -> new IssuedToken(" ", Instant.parse("2026-10-07T12:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }
}
