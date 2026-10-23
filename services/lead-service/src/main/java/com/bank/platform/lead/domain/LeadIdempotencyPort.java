package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.Optional;

/** {@code lead_lms.idempotency_record}: the create command's {@code Idempotency-Key} (S-20). */
public interface LeadIdempotencyPort {

  Optional<IdempotencyRecord> find(String idempotencyKey, Instant now);

  /** Throws {@link ConcurrentLeadUpdateException} if another request claimed the key first. */
  void save(IdempotencyRecord record);

  record IdempotencyRecord(
      String idempotencyKey,
      String requestHash,
      String responseRef,
      Instant createdAt,
      Instant expiresAt) {}
}
