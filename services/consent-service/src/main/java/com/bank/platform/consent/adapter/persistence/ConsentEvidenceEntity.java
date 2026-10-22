package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentChannel;
import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.ConsentEvidence;
import com.bank.platform.consent.domain.ConsentState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consent_evidence")
public class ConsentEvidenceEntity {

  @Id
  @Column(name = "evidence_id", nullable = false, length = 26)
  private String evidenceId;

  @Column(name = "root_consent_id", nullable = false, length = 26)
  private String rootConsentId;

  @Column(name = "record_version", nullable = false)
  private int recordVersion;

  @Column(name = "supersedes_evidence_id", length = 26)
  private String supersedesEvidenceId;

  @Column(name = "customer_id", nullable = false, length = 64)
  private String customerId;

  @Column(name = "consent_code", nullable = false, length = 16)
  private String consentCode;

  @Column(name = "recorded_state", nullable = false, length = 16)
  private String recordedState;

  @Column(name = "valid_from", nullable = false)
  private Instant validFrom;

  @Column(name = "valid_until")
  private Instant validUntil;

  @Column(name = "counterparty_id", length = 64)
  private String counterpartyId;

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

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "cif_enc", nullable = false)
  private byte[] cifEnc;

  @Column(name = "encryption_key_id", nullable = false, length = 64)
  private String encryptionKeyId;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "contact_ref_enc", nullable = false)
  private byte[] contactRefEnc;

  @Column(name = "lead_id", nullable = false, length = 64)
  private String leadId;

  @Column(name = "journey_id", nullable = false, length = 64)
  private String journeyId;

  @Column(name = "channel", nullable = false, length = 16)
  private String channel;

  @Column(name = "otp_transaction_id", nullable = false, length = 26)
  private String otpTransactionId;

  @Column(name = "otp_verified_at", nullable = false)
  private Instant otpVerifiedAt;

  @Column(name = "captured_at", nullable = false)
  private Instant capturedAt;

  @Column(name = "source_ip", length = 64)
  private String sourceIp;

  @Column(name = "captured_by_actor_id", nullable = false, length = 64)
  private String capturedByActorId;

  @Column(name = "captured_by_agent_id", length = 64)
  private String capturedByAgentId;

  @Column(name = "distributor_id", nullable = false, length = 64)
  private String distributorId;

  @Column(name = "lob", nullable = false, length = 16)
  private String lob;

  @Column(name = "config_version_ref", nullable = false, length = 64)
  private String configVersionRef;

  @Column(name = "withdrawal_reason_code", length = 64)
  private String withdrawalReasonCode;

  @Column(name = "ops_task_hint", length = 80)
  private String opsTaskHint;

  @Column(name = "acknowledgement_method", nullable = false, length = 40)
  private String acknowledgementMethod;

  protected ConsentEvidenceEntity() {}

  public static ConsentEvidenceEntity from(ConsentEvidence row) {
    ConsentEvidenceEntity entity = new ConsentEvidenceEntity();
    entity.evidenceId = row.evidenceId();
    entity.rootConsentId = row.rootConsentId();
    entity.recordVersion = row.recordVersion();
    entity.supersedesEvidenceId = row.supersedesEvidenceId();
    entity.customerId = row.customerId();
    entity.consentCode = row.code().wire();
    entity.recordedState = row.recordedState().name();
    entity.validFrom = row.validFrom();
    entity.validUntil = row.validUntil();
    entity.counterpartyId = row.counterpartyId();
    entity.statementId = row.statementId();
    entity.statementVersion = row.statementVersion();
    entity.statementText = row.statementText();
    entity.statementTextHash = row.statementTextHash();
    entity.language = row.language();
    entity.cifEnc = row.cifEnc();
    entity.encryptionKeyId = row.encryptionKeyId();
    entity.contactRefEnc = row.contactRefEnc();
    entity.leadId = row.leadId();
    entity.journeyId = row.journeyId();
    entity.channel = row.channel().name();
    entity.otpTransactionId = row.otpTransactionId();
    entity.otpVerifiedAt = row.otpVerifiedAt();
    entity.capturedAt = row.capturedAt();
    entity.sourceIp = row.sourceIp();
    entity.capturedByActorId = row.capturedByActorId();
    entity.capturedByAgentId = row.capturedByAgentId();
    entity.distributorId = row.distributorId();
    entity.lob = row.lob();
    entity.configVersionRef = row.configVersionRef();
    entity.withdrawalReasonCode = row.withdrawalReasonCode();
    entity.opsTaskHint = row.opsTaskHint();
    entity.acknowledgementMethod = row.acknowledgementMethod();
    return entity;
  }

  public ConsentEvidence toDomain() {
    return new ConsentEvidence(
        evidenceId,
        rootConsentId,
        recordVersion,
        supersedesEvidenceId,
        customerId,
        ConsentCode.parse(consentCode),
        ConsentState.valueOf(recordedState),
        validFrom,
        validUntil,
        counterpartyId,
        statementId,
        statementVersion,
        statementText,
        statementTextHash,
        language,
        cifEnc,
        encryptionKeyId,
        contactRefEnc,
        leadId,
        journeyId,
        ConsentChannel.valueOf(channel),
        otpTransactionId,
        otpVerifiedAt,
        capturedAt,
        sourceIp,
        capturedByActorId,
        capturedByAgentId,
        distributorId,
        lob,
        configVersionRef,
        withdrawalReasonCode,
        opsTaskHint,
        acknowledgementMethod);
  }
}
