package com.bank.platform.suitability.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "assessment", schema = "suitability")
public class AssessmentEntity {

  @Id
  @Column(name = "assessment_id", length = 26)
  private String assessmentId;

  @Column(name = "lead_id", nullable = false, length = 26)
  private String leadId;

  @Column(name = "journey_id", nullable = false, length = 26)
  private String journeyId;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(name = "product_type", nullable = false, length = 32)
  private String productType;

  @Column(name = "proposer_name", nullable = false, length = 120)
  private String proposerName;

  @Column(name = "proposer_dob", nullable = false)
  private LocalDate proposerDob;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(name = "last_screen", nullable = false, length = 40)
  private String lastScreen;

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

  @Column(name = "sp_actor_id", nullable = false, length = 64)
  private String spActorId;

  @Column(name = "rm_actor_id", nullable = false, length = 64)
  private String rmActorId;

  @Column(name = "lock_actor_id", length = 64)
  private String lockActorId;

  @Column(name = "lock_until")
  private Instant lockUntil;

  @Column(name = "premium_inr")
  private Long premiumInr;

  @Column(name = "premium_frequency", length = 16)
  private String premiumFrequency;

  @Column(name = "premium_paying_term", length = 16)
  private String premiumPayingTerm;

  @Column(name = "policy_term", length = 16)
  private String policyTerm;

  @Column(name = "derivation_rule_id", length = 40)
  private String derivationRuleId;

  @Column(name = "consent_state", nullable = false, length = 16)
  private String consentState;

  @Column(name = "awaiting_consent", nullable = false)
  private boolean awaitingConsent;

  @Column(name = "mapping_version", length = 40)
  private String mappingVersion;

  @Column(name = "mapping_run_no", nullable = false)
  private int mappingRunNo;

  @Column(name = "show_products", nullable = false)
  private boolean showProducts;

  @Column(name = "selected_product_id", length = 64)
  private String selectedProductId;

  @Column(name = "bi_reference", length = 64)
  private String biReference;

  @Column(name = "response_version", nullable = false)
  private int responseVersion;

  @Column(nullable = false, length = 8000)
  private String answers;

  @Column(length = 400)
  private String notice;

  @Column(name = "created_by", nullable = false, length = 64)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_by", nullable = false, length = 64)
  private String updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
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

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getProductType() {
    return productType;
  }

  public void setProductType(String productType) {
    this.productType = productType;
  }

  public String getProposerName() {
    return proposerName;
  }

  public void setProposerName(String proposerName) {
    this.proposerName = proposerName;
  }

  public LocalDate getProposerDob() {
    return proposerDob;
  }

  public void setProposerDob(LocalDate proposerDob) {
    this.proposerDob = proposerDob;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLastScreen() {
    return lastScreen;
  }

  public void setLastScreen(String lastScreen) {
    this.lastScreen = lastScreen;
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

  public String getSpActorId() {
    return spActorId;
  }

  public void setSpActorId(String spActorId) {
    this.spActorId = spActorId;
  }

  public String getRmActorId() {
    return rmActorId;
  }

  public void setRmActorId(String rmActorId) {
    this.rmActorId = rmActorId;
  }

  public String getLockActorId() {
    return lockActorId;
  }

  public void setLockActorId(String lockActorId) {
    this.lockActorId = lockActorId;
  }

  public Instant getLockUntil() {
    return lockUntil;
  }

  public void setLockUntil(Instant lockUntil) {
    this.lockUntil = lockUntil;
  }

  public Long getPremiumInr() {
    return premiumInr;
  }

  public void setPremiumInr(Long premiumInr) {
    this.premiumInr = premiumInr;
  }

  public String getPremiumFrequency() {
    return premiumFrequency;
  }

  public void setPremiumFrequency(String premiumFrequency) {
    this.premiumFrequency = premiumFrequency;
  }

  public String getPremiumPayingTerm() {
    return premiumPayingTerm;
  }

  public void setPremiumPayingTerm(String premiumPayingTerm) {
    this.premiumPayingTerm = premiumPayingTerm;
  }

  public String getPolicyTerm() {
    return policyTerm;
  }

  public void setPolicyTerm(String policyTerm) {
    this.policyTerm = policyTerm;
  }

  public String getDerivationRuleId() {
    return derivationRuleId;
  }

  public void setDerivationRuleId(String derivationRuleId) {
    this.derivationRuleId = derivationRuleId;
  }

  public String getConsentState() {
    return consentState;
  }

  public void setConsentState(String consentState) {
    this.consentState = consentState;
  }

  public boolean isAwaitingConsent() {
    return awaitingConsent;
  }

  public void setAwaitingConsent(boolean awaitingConsent) {
    this.awaitingConsent = awaitingConsent;
  }

  public String getMappingVersion() {
    return mappingVersion;
  }

  public void setMappingVersion(String mappingVersion) {
    this.mappingVersion = mappingVersion;
  }

  public int getMappingRunNo() {
    return mappingRunNo;
  }

  public void setMappingRunNo(int mappingRunNo) {
    this.mappingRunNo = mappingRunNo;
  }

  public boolean isShowProducts() {
    return showProducts;
  }

  public void setShowProducts(boolean showProducts) {
    this.showProducts = showProducts;
  }

  public String getSelectedProductId() {
    return selectedProductId;
  }

  public void setSelectedProductId(String selectedProductId) {
    this.selectedProductId = selectedProductId;
  }

  public String getBiReference() {
    return biReference;
  }

  public void setBiReference(String biReference) {
    this.biReference = biReference;
  }

  public int getResponseVersion() {
    return responseVersion;
  }

  public void setResponseVersion(int responseVersion) {
    this.responseVersion = responseVersion;
  }

  public String getAnswers() {
    return answers;
  }

  public void setAnswers(String answers) {
    this.answers = answers;
  }

  public String getNotice() {
    return notice;
  }

  public void setNotice(String notice) {
    this.notice = notice;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
