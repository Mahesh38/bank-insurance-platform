package com.bank.platform.consent.domain;

import java.time.Instant;

/** One secure link issued for BRD 9.10. The raw token is never stored. */
public record ConsentLink(
    String linkId,
    String tokenHash,
    String customerId,
    String leadId,
    String journeyId,
    String assessmentId,
    String mobileEncKeyId,
    byte[] mobileEnc,
    byte[] emailEnc,
    String mobileMasked,
    String emailMasked,
    String state,
    Instant expiresAt,
    Instant issuedAt,
    Instant decidedAt,
    String sourceIp,
    String consentId,
    String actorId,
    String configVersionRef) {

  public ConsentLink decide(String newState, Instant at, String ip, String grantedConsentId) {
    return new ConsentLink(
        linkId,
        tokenHash,
        customerId,
        leadId,
        journeyId,
        assessmentId,
        mobileEncKeyId,
        mobileEnc,
        emailEnc,
        mobileMasked,
        emailMasked,
        newState,
        expiresAt,
        issuedAt,
        at,
        ip,
        grantedConsentId,
        actorId,
        configVersionRef);
  }

  public ConsentLink supersede() {
    return new ConsentLink(
        linkId,
        tokenHash,
        customerId,
        leadId,
        journeyId,
        assessmentId,
        mobileEncKeyId,
        mobileEnc,
        emailEnc,
        mobileMasked,
        emailMasked,
        "SUPERSEDED",
        expiresAt,
        issuedAt,
        decidedAt,
        sourceIp,
        consentId,
        actorId,
        configVersionRef);
  }
}
