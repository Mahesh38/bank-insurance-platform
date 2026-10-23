package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_record", schema = "consent")
@IdClass(ConsentRecordKey.class)
public class ConsentRecordEntity {

  @Id
  @Column(name = "consent_id", length = 26)
  private String consentId;

  @Id
  @Column(name = "record_version")
  private int recordVersion;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(name = "lead_id", length = 26)
  private String leadId;

  @Column(name = "journey_id", length = 26)
  private String journeyId;

  @Column(nullable = false, length = 16)
  private String lob;

  @Column(nullable = false, length = 16)
  private String purpose;

  @Column(name = "scope_insurer_code", length = 32)
  private String insurerCode;

  @Column(name = "statement_id", nullable = false, length = 64)
  private String statementId;

  @Column(name = "statement_version", nullable = false, length = 32)
  private String statementVersion;

  @Column(name = "statement_text", nullable = false, length = 4000)
  private String statementText;

  @Column(name = "statement_text_hash", nullable = false, length = 64)
  private String statementTextHash;

  @Column(nullable = false, length = 8)
  private String language;

  @Column(nullable = false, length = 24)
  private String channel;

  @Column(name = "acknowledgement_method", nullable = false, length = 40)
  private String acknowledgementMethod;

  @Column(nullable = false, length = 48)
  private String checkpoint;

  @Column(nullable = false, length = 20)
  private String state;

  @Column(name = "captured_at", nullable = false)
  private Instant capturedAt;

  @Column(name = "otp_verified_at", nullable = false)
  private Instant otpVerifiedAt;

  @Column(name = "otp_txn_id", nullable = false, length = 64)
  private String otpTxnId;

  @Column(name = "source_ip", nullable = false, length = 64)
  private String sourceIp;

  @Column(name = "contact_used_enc", nullable = false)
  private byte[] contactUsedEnc;

  @Column(name = "encryption_key_id", nullable = false, length = 50)
  private String encryptionKeyId;

  @Column(name = "captured_by_actor_id", nullable = false, length = 64)
  private String capturedByActorId;

  @Column(name = "valid_from", nullable = false)
  private Instant validFrom;

  @Column(name = "valid_until")
  private Instant validUntil;

  @Column(name = "supersedes_consent_id", length = 26)
  private String supersedesConsentId;

  @Column(name = "withdrawn_at")
  private Instant withdrawnAt;

  @Column(name = "withdrawn_by", length = 64)
  private String withdrawnBy;

  @Column(name = "withdrawal_channel", length = 24)
  private String withdrawalChannel;

  @Column(name = "withdrawal_reason_code", length = 40)
  private String withdrawalReasonCode;

  @Column(name = "withdrawal_otp_txn_id", length = 64)
  private String withdrawalOtpTxnId;

  @Column(name = "config_version_ref", nullable = false, length = 128)
  private String configVersionRef;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getConsentId() {
    return consentId;
  }

  public void setConsentId(String consentId) {
    this.consentId = consentId;
  }

  public int getRecordVersion() {
    return recordVersion;
  }

  public void setRecordVersion(int recordVersion) {
    this.recordVersion = recordVersion;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getLeadId() {
    return leadId;
  }

  public void setLeadId(String leadId) {
    this.leadId = leadId;
  }

  public String getJourneyId() {
    return journeyId;
  }

  public void setJourneyId(String journeyId) {
    this.journeyId = journeyId;
  }

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public void setInsurerCode(String insurerCode) {
    this.insurerCode = insurerCode;
  }

  public String getStatementId() {
    return statementId;
  }

  public void setStatementId(String statementId) {
    this.statementId = statementId;
  }

  public String getStatementVersion() {
    return statementVersion;
  }

  public void setStatementVersion(String statementVersion) {
    this.statementVersion = statementVersion;
  }

  public String getStatementText() {
    return statementText;
  }

  public void setStatementText(String statementText) {
    this.statementText = statementText;
  }

  public String getStatementTextHash() {
    return statementTextHash;
  }

  public void setStatementTextHash(String statementTextHash) {
    this.statementTextHash = statementTextHash;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getAcknowledgementMethod() {
    return acknowledgementMethod;
  }

  public void setAcknowledgementMethod(String acknowledgementMethod) {
    this.acknowledgementMethod = acknowledgementMethod;
  }

  public String getCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(String checkpoint) {
    this.checkpoint = checkpoint;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Instant getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(Instant capturedAt) {
    this.capturedAt = capturedAt;
  }

  public Instant getOtpVerifiedAt() {
    return otpVerifiedAt;
  }

  public void setOtpVerifiedAt(Instant otpVerifiedAt) {
    this.otpVerifiedAt = otpVerifiedAt;
  }

  public String getOtpTxnId() {
    return otpTxnId;
  }

  public void setOtpTxnId(String otpTxnId) {
    this.otpTxnId = otpTxnId;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
  }

  public byte[] getContactUsedEnc() {
    return contactUsedEnc;
  }

  public void setContactUsedEnc(byte[] contactUsedEnc) {
    this.contactUsedEnc = contactUsedEnc;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public String getCapturedByActorId() {
    return capturedByActorId;
  }

  public void setCapturedByActorId(String capturedByActorId) {
    this.capturedByActorId = capturedByActorId;
  }

  public Instant getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(Instant validFrom) {
    this.validFrom = validFrom;
  }

  public Instant getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(Instant validUntil) {
    this.validUntil = validUntil;
  }

  public String getSupersedesConsentId() {
    return supersedesConsentId;
  }

  public void setSupersedesConsentId(String supersedesConsentId) {
    this.supersedesConsentId = supersedesConsentId;
  }

  public Instant getWithdrawnAt() {
    return withdrawnAt;
  }

  public void setWithdrawnAt(Instant withdrawnAt) {
    this.withdrawnAt = withdrawnAt;
  }

  public String getWithdrawnBy() {
    return withdrawnBy;
  }

  public void setWithdrawnBy(String withdrawnBy) {
    this.withdrawnBy = withdrawnBy;
  }

  public String getWithdrawalChannel() {
    return withdrawalChannel;
  }

  public void setWithdrawalChannel(String withdrawalChannel) {
    this.withdrawalChannel = withdrawalChannel;
  }

  public String getWithdrawalReasonCode() {
    return withdrawalReasonCode;
  }

  public void setWithdrawalReasonCode(String withdrawalReasonCode) {
    this.withdrawalReasonCode = withdrawalReasonCode;
  }

  public String getWithdrawalOtpTxnId() {
    return withdrawalOtpTxnId;
  }

  public void setWithdrawalOtpTxnId(String withdrawalOtpTxnId) {
    this.withdrawalOtpTxnId = withdrawalOtpTxnId;
  }

  public String getConfigVersionRef() {
    return configVersionRef;
  }

  public void setConfigVersionRef(String configVersionRef) {
    this.configVersionRef = configVersionRef;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
