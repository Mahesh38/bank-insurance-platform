package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;

@Tag("unit")
class DownstreamTimeoutsTest {

  @Test
  void defaultsFollowTheSeamCatalogue() {
    DownstreamTimeouts timeouts = bind(Map.of());

    assertThat(timeouts.lead().read()).isEqualTo(Duration.ofSeconds(2));
    assertThat(timeouts.customer().read()).isEqualTo(Duration.ofSeconds(4));
    assertThat(timeouts.authorization().read()).isEqualTo(Duration.ofSeconds(2));
    assertThat(timeouts.identityProvider().read()).isEqualTo(Duration.ofSeconds(6));
  }

  @Test
  void eachBudgetIsOverridable() {
    DownstreamTimeouts timeouts = bind(Map.of("workforce.downstream.timeouts.lead.read", "750ms"));

    assertThat(timeouts.lead().read()).isEqualTo(Duration.ofMillis(750));
    assertThat(timeouts.lead().connect()).isEqualTo(Duration.ofMillis(500));
    assertThat(timeouts.customer().read()).isEqualTo(Duration.ofSeconds(4));
  }

  private static DownstreamTimeouts bind(Map<String, String> values) {
    return new Binder(
            List.of(new MapConfigurationPropertySource(values)),
            null,
            ApplicationConversionService.getSharedInstance())
        .bindOrCreate("workforce.downstream.timeouts", DownstreamTimeouts.class);
  }
}
