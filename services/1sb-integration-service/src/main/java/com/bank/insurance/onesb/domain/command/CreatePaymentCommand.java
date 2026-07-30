package com.bank.insurance.onesb.domain.command;

import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Command for {@code POST /v1/payments} (FUNC-007).
 */
public record CreatePaymentCommand(
        String applicationNumber,
        String proposalJobId,
        String redirectUrl,
        Lob lob,
        String journeyId,
        String idempotencyKey,
        String actorId
) {}
