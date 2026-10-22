package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ChallengePurpose;
import com.bank.platform.consent.domain.ChallengeStatus;
import com.bank.platform.consent.domain.ConsentChannel;
import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.PendingCapture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "otp_challenge")
public class OtpChallengeEntity {

  @Id
  @Column(name = "challenge_id", nullable = false, length = 26)
  private String challengeId;

  @Column(name = "purpose", nullable = false, length = 16)
  private String purpose;

  @Column(name = "root_consent_id", nullable = false, length = 26)
  private String rootConsentId;

  @Column(name = "customer_id", nullable = false, length = 64)
  private String customerId;

  @Column(name = "consent_code", nullable = false, length = 16)
  private String consentCode;

  @Column(name = "otp_hash", nullable = false, length = 64)
  private String otpHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "status", nullable = false, length = 16)
  private String status;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "cif_enc", nullable = false)
  private byte[] cifEnc;

  @Column(name = "encryption_key_id", nullable = false, length = 64)
  private String encryptionKeyId;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "contact_ref_enc", nullable = false)
  private byte[] contactRefEnc;

  @Column(name = "lead_id", length = 64)
  private String leadId;

  @Column(name = "journey_id", length = 64)
  private String journeyId;

  @Column(name = "statement_id", nullable = false, length = 64)
  private String statementId;

  @Column(name = "statement_version", nullable = false, length = 16)
  private String statementVersion;

  @Column(name = "statement_text", nullable = false, length = 8000)
  private String statementText;

  @Column(name = "statement_text_hash", nullable = false, length = 64)
  private String statementTextHash;

  @Column(name = "language", nullable = false, length = 8)
  private String language;

  @Column(name = "channel", nullable = false, length = 16)
  private String channel;

  @Column(name = "captured_by_actor_id", length = 64)
  private String capturedByActorId;

  @Column(name = "captured_by_agent_id", length = 64)
  private String capturedByAgentId;

  @Column(name = "distributor_id", nullable = false, length = 64)
  private String distributorId;

  @Column(name = "counterparty_id", length = 64)
  private String counterpartyId;

  @Column(name = "lob", nullable = false, length = 16)
  private String lob;

  @Column(name = "config_version_ref", length = 64)
  private String configVersionRef;

  @Column(name = "withdrawal_reason_code", length = 64)
  private String withdrawalReasonCode;

  @Column(name = "proposal_submitted", nullable = false)
  private boolean proposalSubmitted;

  @Column(name = "source_ip", length = 64)
  private String sourceIp;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OtpChallengeEntity() {}

  public static OtpChallengeEntity from(OtpChallenge challenge) {
    PendingCapture pending = challenge.pending();
    OtpChallengeEntity entity = new OtpChallengeEntity();
    entity.challengeId = challenge.challengeId();
    entity.purpose = challenge.purpose().name();
    entity.rootConsentId = challenge.rootConsentId();
    entity.customerId = challenge.customerId();
    entity.consentCode = challenge.code().wire();
    entity.otpHash = challenge.otpHash();
    entity.expiresAt = challenge.expiresAt();
    entity.attempts = challenge.attempts();
    entity.status = challenge.status().name();
    entity.cifEnc = pending.cifEnc();
    entity.encryptionKeyId = pending.encryptionKeyId();
    entity.contactRefEnc = pending.contactRefEnc();
    entity.leadId = pending.leadId();
    entity.journeyId = pending.journeyId();
    entity.statementId = pending.statementId();
    entity.statementVersion = pending.statementVersion();
    entity.statementText = pending.statementText();
    entity.statementTextHash = pending.statementTextHash();
    entity.language = pending.language();
    entity.channel = pending.channel().name();
    entity.capturedByActorId = pending.capturedByActorId();
    entity.capturedByAgentId = pending.capturedByAgentId();
    entity.distributorId = pending.distributorId();
    entity.counterpartyId = pending.counterpartyId();
    entity.lob = pending.lob();
    entity.configVersionRef = pending.configVersionRef();
    entity.withdrawalReasonCode = pending.withdrawalReasonCode();
    entity.proposalSubmitted = pending.proposalSubmitted();
    entity.sourceIp = pending.sourceIp();
    entity.createdAt = challenge.createdAt();
    return entity;
  }

  public void advance(int nextAttempts, String nextStatus) {
    this.attempts = nextAttempts;
    this.status = nextStatus;
  }

  public OtpChallenge toDomain() {
    PendingCapture pending =
        new PendingCapture(
            cifEnc,
            encryptionKeyId,
            contactRefEnc,
            leadId,
            journeyId,
            statementId,
            statementVersion,
            statementText,
            statementTextHash,
            language,
            ConsentChannel.valueOf(channel),
            capturedByActorId,
            capturedByAgentId,
            distributorId,
            counterpartyId,
            lob,
            configVersionRef,
            withdrawalReasonCode,
            proposalSubmitted,
            sourceIp);
    return new OtpChallenge(
        challengeId,
        ChallengePurpose.valueOf(purpose),
        rootConsentId,
        customerId,
        ConsentCode.parse(consentCode),
        otpHash,
        expiresAt,
        attempts,
        ChallengeStatus.valueOf(status),
        pending,
        createdAt);
  }
}
