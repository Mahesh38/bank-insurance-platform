package com.bank.platform.consent.api;

import java.util.List;

/**
 * Responses omit ciphertext, the distributor secret, the source address and the statement text.
 * Recorded state is what was stored. Effective status is derived and is never written back.
 */
public final class ConsentViews {

  private ConsentViews() {}

  public record StatementView(
      String statementId, String version, String textHash, String language, String changeClass) {}

  public record ChallengeView(
      String challengeId, String purpose, String rootConsentId, String expiresAt, String status) {}

  public record EvidenceView(
      String evidenceId,
      String rootConsentId,
      int recordVersion,
      String supersedesEvidenceId,
      String customerId,
      String consentCode,
      String recordedState,
      String effectiveStatus,
      String validFrom,
      String validUntil,
      String counterpartyId,
      String statementId,
      String statementVersion,
      String statementTextHash,
      String language,
      String leadId,
      String journeyId,
      String channel,
      String acknowledgementMethod,
      String withdrawalReasonCode,
      String opsTaskHint,
      String lob) {}

  public record GrantView(
      boolean challengeIssued, ChallengeView challenge, EvidenceView current, String effect) {}

  public record EvaluationView(
      boolean allowed,
      boolean journeyBlocked,
      String errorCode,
      String effect,
      String consentId,
      String consentCode) {}

  public record ChainView(String rootConsentId, List<EvidenceView> records) {}

  public record RetentionView(String rootConsentId, String terminalAt, String retainUntil) {}
}
