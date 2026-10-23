package com.bank.platform.proposal.domain;

import java.time.Instant;

/**
 * An OUTBOX item not yet marked published, as the sweeper reads it (ADR-025 §3).
 *
 * @param partitionKey the aggregate's item key ({@code PROPOSAL#{proposalId}}); order is kept per
 *     key
 * @param envelopeJson the stored {@code event-envelope/v1} JSON, published unchanged
 */
public record PendingOutboxItem(
    String partitionKey, String eventId, Instant occurredAt, String envelopeJson) {}
