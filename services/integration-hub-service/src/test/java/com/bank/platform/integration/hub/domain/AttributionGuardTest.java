package com.bank.platform.integration.hub.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-032")
class AttributionGuardTest {

  @Test
  void rejectsNestedDistribution_AT_HUB_04() {
    assertThat(
            AttributionGuard.containsAttribution(
                Map.of("body", Map.of("distribution", Map.of("agentId", "A1")))))
        .isTrue();
  }

  @Test
  void allowsCleanQuoteBody() {
    assertThat(
            AttributionGuard.containsAttribution(
                Map.of(
                    "operation",
                    "QUOTE",
                    "body",
                    Map.of(
                        "category",
                        "PREMIUM",
                        "members",
                        List.of(Map.of("role", "LIFE_ASSURED"))))))
        .isFalse();
  }
}
