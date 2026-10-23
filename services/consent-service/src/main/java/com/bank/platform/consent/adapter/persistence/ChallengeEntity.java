package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_challenge", schema = "consent")
public class ChallengeEntity {

  @Id
  @Column(name = "challenge_id", length = 26)
  private String challengeId;

  @Column(nullable = false, length = 16)
  private String kind;

  @Column(nullable = false, length = 16)
  private String state;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "otp_issue_count", nullable = false)
  private int otpIssueCount;

  @Column(name = "otp_expires_at", nullable = false)
  private Instant otpExpiresAt;

  @Column(name = "otp_txn_id", nullable = false, length = 64)
  private String otpTxnId;

  @Column(name = "otp_hash", nullable = false, length = 128)
  private String otpHash;

  @Column(nullable = false, length = 80)
  private String purposes;

  @Column(name = "statement_refs", nullable = false, length = 4000)
  private String statementRefs;

  @Column(name = "contact_used_enc", nullable = false)
  private byte[] contactUsedEnc;

  @Column(name = "encryption_key_id", nullable = false, length = 50)
  private String encryptionKeyId;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(name = "journey_id", length = 26)
  private String journeyId;

  @Column(name = "lead_id", length = 26)
  private String leadId;

  @Column(nullable = false, length = 16)
  private String lob;

  @Column(nullable = false, length = 48)
  private String checkpoint;

  @Column(name = "insurer_code", length = 32)
  private String insurerCode;

  @Column(nullable = false, length = 24)
  private String channel;

  @Column(nullable = false, length = 8)
  private String language;

  @Column(name = "target_consent_id", length = 26)
  private String targetConsentId;

  @Column(name = "actor_id", nullable = false, length = 64)
  private String actorId;

  @Column(name = "config_version_ref", nullable = false, length = 128)
  private String configVersionRef;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getChallengeId() {
    return challengeId;
  }

  public void setChallengeId(String challengeId) {
    this.challengeId = challengeId;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public int getOtpIssueCount() {
    return otpIssueCount;
  }

  public void setOtpIssueCount(int otpIssueCount) {
    this.otpIssueCount = otpIssueCount;
  }

  public Instant getOtpExpiresAt() {
    return otpExpiresAt;
  }

  public void setOtpExpiresAt(Instant otpExpiresAt) {
    this.otpExpiresAt = otpExpiresAt;
  }

  public String getOtpTxnId() {
    return otpTxnId;
  }

  public void setOtpTxnId(String otpTxnId) {
    this.otpTxnId = otpTxnId;
  }

  public String getOtpHash() {
    return otpHash;
  }

  public void setOtpHash(String otpHash) {
    this.otpHash = otpHash;
  }

  public String getPurposes() {
    return purposes;
  }

  public void setPurposes(String purposes) {
    this.purposes = purposes;
  }

  public String getStatementRefs() {
    return statementRefs;
  }

  public void setStatementRefs(String statementRefs) {
    this.statementRefs = statementRefs;
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

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getJourneyId() {
    return journeyId;
  }

  public void setJourneyId(String journeyId) {
    this.journeyId = journeyId;
  }

  public String getLeadId() {
    return leadId;
  }

  public void setLeadId(String leadId) {
    this.leadId = leadId;
  }

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(String checkpoint) {
    this.checkpoint = checkpoint;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public void setInsurerCode(String insurerCode) {
    this.insurerCode = insurerCode;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTargetConsentId() {
    return targetConsentId;
  }

  public void setTargetConsentId(String targetConsentId) {
    this.targetConsentId = targetConsentId;
  }

  public String getActorId() {
    return actorId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
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
