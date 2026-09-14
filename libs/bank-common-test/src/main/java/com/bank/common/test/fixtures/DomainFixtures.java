package com.bank.common.test.fixtures;

import java.util.UUID;

/** S08-E03-S03 — shared synthetic domain identifiers for tests. No PII. */
public final class DomainFixtures {

  private DomainFixtures() {}

  public static String journeyId() {
    // persistence.journey_id is varchar(36) — keep within UUID length
    return UUID.randomUUID().toString();
  }

  public static String idempotencyKey(String prefix) {
    String raw = prefix + "-" + UUID.randomUUID();
    return raw.length() <= 64 ? raw : UUID.randomUUID().toString();
  }

  public static String actorId() {
    // created_by_actor is typically varchar(64); keep short and stable-looking
    return "actor-" + UUID.randomUUID().toString().substring(0, 8);
  }

  public static String jobId() {
    return UUID.randomUUID().toString();
  }
}
