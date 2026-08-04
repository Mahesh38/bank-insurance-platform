package com.bank.insurance.onesb.domain.model;

/**
 * Raw 1SB application-status outcome (adapter → application) before bank normalisation.
 * {@code rawStatus} is the 1SB {@code applicationStatus} enum value; {@code subStatus} is the
 * manufacturer-level detail surfaced to RMs.
 */
public record OneSbApplicationStatusResult(
        String applicationNumber,
        String rawStatus,
        String subStatus,
        String policyNumber
) {}
