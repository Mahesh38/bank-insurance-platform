package com.bank.insurance.onesb.domain.model;

/**
 * Normalised 1SB proposal-submit outcome (adapter → application).
 * When {@code applicationNumber} is present the job can complete immediately;
 * otherwise poll using {@code externalReqId}.
 */
public record OneSbProposalSubmitResult(
        String externalReqId,
        String applicationNumber,
        boolean complete
) {}
