package com.bank.platform.proposal.domain;

import java.time.Instant;

/**
 * The {@code idempotency_record} row that dedupes a {@code PaymentReconciled} event; {@code eventId}
 * is also {@code proposal.materialized_event_id} and the PAID history row's {@code source_event_id}.
 */
public record MaterializationReceipt(
    String eventId, String requestHash, Instant expiresAt, String paymentId) {}
