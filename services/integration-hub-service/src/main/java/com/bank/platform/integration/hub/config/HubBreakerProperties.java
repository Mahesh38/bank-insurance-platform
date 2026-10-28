package com.bank.platform.integration.hub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hub.breaker")
public record HubBreakerProperties(
    boolean enabled, int failureThreshold, long openWaitMs, int halfOpenSuccessThreshold) {

  public HubBreakerProperties {
    if (failureThreshold <= 0) {
      failureThreshold = 3;
    }
    if (openWaitMs <= 0) {
      openWaitMs = 30_000L;
    }
    if (halfOpenSuccessThreshold <= 0) {
      halfOpenSuccessThreshold = 1;
    }
  }
}
