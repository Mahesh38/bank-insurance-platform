package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.Arrays;

/**
 * One append-only evidence row. Withdrawal and re-consent are further rows. Nothing on this object
 * is rewritten after it is stored (INV-CNS-01, CNS-R16, CNS-R17).
 */
public final class ConsentEvidence {

  private final String evidenceId;
  private final String rootConsentId;
  private final int recordVersion;
  private final String supersedesEvidenceId;
  private final String customerId;
  private final ConsentCode code;
  private final ConsentState recordedState;
  private final Instant validFrom;
  private final Instant validUntil;
  private final String counterpartyId;
  private final String statementVersion;
  private final String statementText;
  private final String statementTextHash;
  private final String language;
  private final String statementId;
  private final byte[] cifEnc;
  private final String encryptionKeyId;
  private final byte[] contactRefEnc;
  private final String leadId;
  private final String journeyId;
  private final ConsentChannel channel;
  private final String otpTransactionId;
  private final Instant otpVerifiedAt;
  private final Instant capturedAt;
  private final String sourceIp;
  private final String capturedByActorId;
  private final String capturedByAgentId;
  private final String distributorId;
  private final String lob;
  private final String configVersionRef;
  private final String withdrawalReasonCode;
  private final String opsTaskHint;
  private final String acknowledgementMethod;

  public ConsentEvidence(
      String evidenceId,
      String rootConsentId,
      int recordVersion,
      String supersedesEvidenceId,
      String customerId,
      ConsentCode code,
      ConsentState recordedState,
      Instant validFrom,
      Instant validUntil,
      String counterpartyId,
      String statementId,
      String statementVersion,
      String statementText,
      String statementTextHash,
      String language,
      byte[] cifEnc,
      String encryptionKeyId,
      byte[] contactRefEnc,
      String leadId,
      String journeyId,
      ConsentChannel channel,
      String otpTransactionId,
      Instant otpVerifiedAt,
      Instant capturedAt,
      String sourceIp,
      String capturedByActorId,
      String capturedByAgentId,
      String distributorId,
      String lob,
      String configVersionRef,
      String withdrawalReasonCode,
      String opsTaskHint,
      String acknowledgementMethod) {
    this.evidenceId = evidenceId;
    this.rootConsentId = rootConsentId;
    this.recordVersion = recordVersion;
    this.supersedesEvidenceId = supersedesEvidenceId;
    this.customerId = customerId;
    this.code = code;
    this.recordedState = recordedState;
    this.validFrom = validFrom;
    this.validUntil = validUntil;
    this.counterpartyId = counterpartyId;
    this.statementId = statementId;
    this.statementVersion = statementVersion;
    this.statementText = statementText;
    this.statementTextHash = statementTextHash;
    this.language = language;
    this.cifEnc = cifEnc == null ? new byte[0] : cifEnc.clone();
    this.encryptionKeyId = encryptionKeyId;
    this.contactRefEnc = contactRefEnc == null ? new byte[0] : contactRefEnc.clone();
    this.leadId = leadId;
    this.journeyId = journeyId;
    this.channel = channel;
    this.otpTransactionId = otpTransactionId;
    this.otpVerifiedAt = otpVerifiedAt;
    this.capturedAt = capturedAt;
    this.sourceIp = sourceIp;
    this.capturedByActorId = capturedByActorId;
    this.capturedByAgentId = capturedByAgentId;
    this.distributorId = distributorId;
    this.lob = lob;
    this.configVersionRef = configVersionRef;
    this.withdrawalReasonCode = withdrawalReasonCode;
    this.opsTaskHint = opsTaskHint;
    this.acknowledgementMethod = acknowledgementMethod;
  }

  public String evidenceId() {
    return evidenceId;
  }

  public String rootConsentId() {
    return rootConsentId;
  }

  public int recordVersion() {
    return recordVersion;
  }

  public String supersedesEvidenceId() {
    return supersedesEvidenceId;
  }

  public String customerId() {
    return customerId;
  }

  public ConsentCode code() {
    return code;
  }

  public ConsentState recordedState() {
    return recordedState;
  }

  public Instant validFrom() {
    return validFrom;
  }

  public Instant validUntil() {
    return validUntil;
  }

  public String counterpartyId() {
    return counterpartyId;
  }

  public String statementId() {
    return statementId;
  }

  public String statementVersion() {
    return statementVersion;
  }

  public String statementText() {
    return statementText;
  }

  public String statementTextHash() {
    return statementTextHash;
  }

  public String language() {
    return language;
  }

  public byte[] cifEnc() {
    return cifEnc.clone();
  }

  public String encryptionKeyId() {
    return encryptionKeyId;
  }

  public byte[] contactRefEnc() {
    return contactRefEnc.clone();
  }

  public String leadId() {
    return leadId;
  }

  public String journeyId() {
    return journeyId;
  }

  public ConsentChannel channel() {
    return channel;
  }

  public String otpTransactionId() {
    return otpTransactionId;
  }

  public Instant otpVerifiedAt() {
    return otpVerifiedAt;
  }

  public Instant capturedAt() {
    return capturedAt;
  }

  public String sourceIp() {
    return sourceIp;
  }

  public String capturedByActorId() {
    return capturedByActorId;
  }

  public String capturedByAgentId() {
    return capturedByAgentId;
  }

  public String distributorId() {
    return distributorId;
  }

  public String lob() {
    return lob;
  }

  public String configVersionRef() {
    return configVersionRef;
  }

  public String withdrawalReasonCode() {
    return withdrawalReasonCode;
  }

  public String opsTaskHint() {
    return opsTaskHint;
  }

  public String acknowledgementMethod() {
    return acknowledgementMethod;
  }

  @Override
  public String toString() {
    return "ConsentEvidence{root="
        + rootConsentId
        + ", version="
        + recordVersion
        + ", state="
        + recordedState
        + "}";
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ConsentEvidence evidence)) {
      return false;
    }
    return evidenceId.equals(evidence.evidenceId) && Arrays.equals(cifEnc, evidence.cifEnc);
  }

  @Override
  public int hashCode() {
    return evidenceId.hashCode();
  }
}
