package com.bank.platform.quotation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Outbound RestClient settings for Hub, suitability and consent (FUNC-033). */
@Validated
@ConfigurationProperties("quotation.clients")
public record QuotationClientProperties(Hub hub, Suitability suitability, Consent consent) {

  public QuotationClientProperties {
    hub = hub == null ? new Hub(null, null, 1_000, 3_000, 30_000) : hub;
    suitability = suitability == null ? new Suitability(null, null, 1_000, 500) : suitability;
    consent = consent == null ? new Consent(null, null, 1_000, 500) : consent;
  }

  public record Hub(
      String baseUrl,
      String internalServiceKey,
      int connectTimeoutMs,
      int submitReadTimeoutMs,
      int pollReadTimeoutMs) {

    public Hub {
      if (connectTimeoutMs <= 0) {
        connectTimeoutMs = 1_000;
      }
      if (submitReadTimeoutMs <= 0) {
        submitReadTimeoutMs = 3_000;
      }
      if (pollReadTimeoutMs <= 0) {
        pollReadTimeoutMs = 30_000;
      }
    }
  }

  public record Suitability(
      String baseUrl, String internalServiceKey, int connectTimeoutMs, int readTimeoutMs) {

    public Suitability {
      if (connectTimeoutMs <= 0) {
        connectTimeoutMs = 1_000;
      }
      if (readTimeoutMs <= 0) {
        readTimeoutMs = 500;
      }
    }
  }

  public record Consent(
      String baseUrl, String internalServiceKey, int connectTimeoutMs, int readTimeoutMs) {

    public Consent {
      if (connectTimeoutMs <= 0) {
        connectTimeoutMs = 1_000;
      }
      if (readTimeoutMs <= 0) {
        readTimeoutMs = 500;
      }
    }
  }
}
