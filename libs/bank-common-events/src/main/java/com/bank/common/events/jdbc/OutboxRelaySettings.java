package com.bank.common.events.jdbc;

/**
 * Relay tuning (ADR-025 §3): batches of 100, park after 10 failed attempts.
 *
 * @param dialect null = detect from the connection
 */
public record OutboxRelaySettings(int batchSize, int maxAttempts, SqlDialect dialect) {

  public static final int DEFAULT_BATCH_SIZE = 100;
  public static final int DEFAULT_MAX_ATTEMPTS = 10;

  public OutboxRelaySettings {
    if (batchSize < 1 || batchSize > 1000) {
      throw new IllegalArgumentException("batchSize must be 1..1000");
    }
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >= 1");
    }
  }

  public static OutboxRelaySettings defaults() {
    return new OutboxRelaySettings(DEFAULT_BATCH_SIZE, DEFAULT_MAX_ATTEMPTS, null);
  }
}
