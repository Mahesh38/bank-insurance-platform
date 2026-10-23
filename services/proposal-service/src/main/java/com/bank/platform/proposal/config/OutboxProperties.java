package com.bank.platform.proposal.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox publishing (ADR-025 §3, §5). Technical settings only.
 *
 * @param publisher {@code none} (default: items stay {@code published=false}, nothing is lost),
 *     {@code in-memory} (local / test profiles only) or {@code kafka} (MSK)
 */
@ConfigurationProperties(prefix = "proposal.outbox")
public record OutboxProperties(String publisher, Sweeper sweeper, Kafka kafka) {

  /** The republishing sweeper; ADR-025 §3 fixes the age at 5 minutes, the interval at 1 minute. */
  public record Sweeper(boolean enabled, Duration interval, Duration minAge, int batchSize) {
    public Sweeper {
      interval = interval == null ? Duration.ofMinutes(1) : interval;
      minAge = minAge == null ? Duration.ofMinutes(5) : minAge;
      batchSize = batchSize <= 0 ? 100 : batchSize;
    }
  }

  /** Topic {@code {env}.proposal.proposal.v1} (ADR-025 §2). */
  public record Kafka(String bootstrapServers, String env, Duration ackTimeout) {
    public Kafka {
      ackTimeout = ackTimeout == null ? Duration.ofSeconds(10) : ackTimeout;
    }
  }

  public OutboxProperties {
    publisher = publisher == null || publisher.isBlank() ? "none" : publisher;
    sweeper = sweeper == null ? new Sweeper(false, null, null, 0) : sweeper;
    kafka = kafka == null ? new Kafka(null, null, null) : kafka;
  }
}
