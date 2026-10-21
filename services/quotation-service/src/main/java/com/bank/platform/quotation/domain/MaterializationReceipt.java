package com.bank.platform.quotation.domain;

import java.time.Instant;

/** The {@code idempotency_record} row that dedupes a {@code PaymentReconciled} event. */
public record MaterializationReceipt(String eventId, String requestHash, Instant expiresAt) {}
