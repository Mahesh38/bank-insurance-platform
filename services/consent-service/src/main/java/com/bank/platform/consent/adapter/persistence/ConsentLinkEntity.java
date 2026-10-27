package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_link", schema = "consent")
public class ConsentLinkEntity {

  @Id
  @Column(name = "link_id", length = 26)
  private String linkId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(name = "lead_id", nullable = false, length = 26)
  private String leadId;

  @Column(name = "journey_id", length = 26)
  private String journeyId;

  @Column(name = "assessment_id", nullable = false, length = 26)
  private String assessmentId;

  @Column(name = "encryption_key_id", nullable = false, length = 50)
  private String encryptionKeyId;

  @Column(name = "mobile_enc", nullable = false)
  private byte[] mobileEnc;

  @Column(name = "email_enc", nullable = false)
  private byte[] emailEnc;

  @Column(name = "mobile_masked", nullable = false, length = 32)
  private String mobileMasked;

  @Column(name = "email_masked", nullable = false, length = 80)
  private String emailMasked;

  @Column(nullable = false, length = 16)
  private String state;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "source_ip", length = 64)
  private String sourceIp;

  @Column(name = "consent_id", length = 26)
  private String consentId;

  @Column(name = "actor_id", nullable = false, length = 64)
  private String actorId;

  @Column(name = "config_version_ref", nullable = false, length = 128)
  private String configVersionRef;

  public String getLinkId() {
    return linkId;
  }

  public void setLinkId(String linkId) {
    this.linkId = linkId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
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

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public byte[] getMobileEnc() {
    return mobileEnc;
  }

  public void setMobileEnc(byte[] mobileEnc) {
    this.mobileEnc = mobileEnc;
  }

  public byte[] getEmailEnc() {
    return emailEnc;
  }

  public void setEmailEnc(byte[] emailEnc) {
    this.emailEnc = emailEnc;
  }

  public String getMobileMasked() {
    return mobileMasked;
  }

  public void setMobileMasked(String mobileMasked) {
    this.mobileMasked = mobileMasked;
  }

  public String getEmailMasked() {
    return emailMasked;
  }

  public void setEmailMasked(String emailMasked) {
    this.emailMasked = emailMasked;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(Instant decidedAt) {
    this.decidedAt = decidedAt;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
  }

  public String getConsentId() {
    return consentId;
  }

  public void setConsentId(String consentId) {
    this.consentId = consentId;
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
}
