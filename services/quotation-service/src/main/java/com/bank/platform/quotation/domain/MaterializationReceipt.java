package com.bank.platform.quotation.domain;

import java.time.Instant;

/**
 * The {@code idempotency_record} row that dedupes a {@code PaymentReconciled} event; {@code
 * eventId} is also stored as {@code quote.materialized_event_id}.
 */
public record MaterializationReceipt(
    String eventId, String requestHash, Instant expiresAt, String paymentId) {}
