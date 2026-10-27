package com.bank.platform.suitability.domain;

import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One suitability assessment for a lead. Suitable products appear only after quote-share consent.
 */
public final class Assessment {

  private String assessmentId;
  private String leadId;
  private String journeyId;
  private String customerId;
  private String productType;
  private String proposerName;
  private LocalDate proposerDob;
  private String status;
  private String lastScreen;
  private String encryptionKeyId;
  private byte[] mobileEnc;
  private byte[] emailEnc;
  private String mobileMasked;
  private String emailMasked;
  private String spActorId;
  private String rmActorId;
  private String lockActorId;
  private Instant lockUntil;
  private Long premiumInr;
  private String premiumFrequency;
  private String premiumPayingTerm;
  private String policyTerm;
  private String derivationRuleId;
  private String consentState;
  private boolean awaitingConsent;
  private String mappingVersion;
  private int mappingRunNo;
  private boolean showProducts;
  private String selectedProductId;
  private String biReference;
  private int responseVersion;
  private String answers;
  private String notice;
  private String createdBy;
  private Instant createdAt;
  private String updatedBy;
  private Instant updatedAt;
  private List<ProductMatch> products = new ArrayList<>();

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

  public List<ProductMatch> getProducts() {
    return products;
  }

  public void setProducts(List<ProductMatch> products) {
    this.products = products == null ? new ArrayList<>() : new ArrayList<>(products);
  }
}
