package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.StatementDoc;
import java.time.Instant;
import java.util.List;

/** Commands and views for the consent use cases. The HTTP layer maps onto these records. */
public final class ConsentTypes {

  private ConsentTypes() {}

  public record CaptureCommand(
      String customerId,
      String leadId,
      String journeyId,
      String lob,
      String checkpoint,
      List<String> purposes,
      String insurerCode,
      String channel,
      String language) {}

  public record Affirmation(
      String purpose, String statementId, String statementVersion, Instant affirmedAt) {}

  public record VerifyCommand(
      String challengeId,
      String otpTxnId,
      String otp,
      String verificationMode,
      String sourceIp,
      List<Affirmation> affirmations) {}

  public record CheckView(
      String customerId,
      String checkpoint,
      boolean satisfied,
      boolean captureBlocked,
      String policyRuleSetId,
      String policyVersion,
      List<PurposeResult> valid,
      List<PurposeResult> missing) {}

  public record PurposeResult(
      String consentId,
      String purpose,
      String statementId,
      String statementVersion,
      String insurerCode,
      Instant validUntil,
      String reason,
      boolean missing) {

    static PurposeResult valid(ConsentRow row) {
      return new PurposeResult(
          row.consentId(),
          row.purpose(),
          row.statementId(),
          row.statementVersion(),
          row.insurerCode(),
          row.validUntil(),
          null,
          false);
    }

    static PurposeResult missing(String purpose, String reason, StatementDoc statement) {
      return new PurposeResult(
          null, purpose, statement.statementId(), statement.version(), null, null, reason, true);
    }
  }

  public record ChallengeView(
      String challengeId,
      String kind,
      String state,
      String customerId,
      List<String> purposes,
      Instant otpExpiresAt,
      String maskedContact,
      int attemptsRemaining,
      List<String> consentIds) {}

  public record ConsentView(
      String consentId,
      int recordVersion,
      String customerId,
      String leadId,
      String journeyId,
      String lob,
      String purpose,
      String insurerCode,
      String state,
      String statementId,
      String statementVersion,
      String statementTextHash,
      String language,
      String channel,
      String acknowledgementMethod,
      String checkpoint,
      Instant capturedAt,
      Instant validFrom,
      Instant validUntil,
      String supersedesConsentId,
      Instant withdrawnAt,
      String configVersionRef) {}

  public record ChainView(String consentId, List<ConsentView> rows) {}
}
