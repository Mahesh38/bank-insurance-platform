package com.bank.platform.integration.hub.adapter.onesb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hub.onesb")
public record OnesbClientProperties(
    String baseUrl,
    String internalServiceKey,
    int connectTimeoutMs,
    int submitReadTimeoutMs,
    int pollReadTimeoutMs) {

  public OnesbClientProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "http://localhost:8080";
    }
    if (internalServiceKey == null || internalServiceKey.isBlank()) {
      internalServiceKey = "local-onesb-hub-internal-key";
    }
    if (connectTimeoutMs <= 0) {
      connectTimeoutMs = 2_000;
    }
    if (submitReadTimeoutMs <= 0) {
      submitReadTimeoutMs = 25_000;
    }
    if (pollReadTimeoutMs <= 0) {
      pollReadTimeoutMs = 2_000;
    }
  }
}
