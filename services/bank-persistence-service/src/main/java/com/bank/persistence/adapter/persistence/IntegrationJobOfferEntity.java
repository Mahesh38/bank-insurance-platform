package com.bank.persistence.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "integration_job_offer")
public class IntegrationJobOfferEntity {

  @Id
  @Column(name = "offer_id", length = 36, nullable = false)
  private String offerId;

  @Column(name = "job_id", length = 36, nullable = false)
  private String jobId;

  @Column(name = "insurer_code", length = 50)
  private String insurerCode;

  @Column(name = "product_code", length = 100)
  private String productCode;

  @Column(name = "product_name", length = 200)
  private String productName;

  @Column(name = "premium_amount", precision = 15, scale = 2)
  private BigDecimal premiumAmount;

  @Column(name = "premium_frequency", length = 10)
  private String premiumFrequency;

  @Column(name = "sum_assured", precision = 15, scale = 2)
  private BigDecimal sumAssured;

  @Column(name = "out_of_bound")
  private Boolean outOfBound = false;

  @Column(name = "offer_status", length = 20)
  private String offerStatus;

  @Column(name = "error_summary", length = 500)
  private String errorSummary;

  @Column(name = "raw_offer_blob_id", length = 36)
  private String rawOfferBlobId;

  /** JSON array of bank {@code FundAllocation} rows; null when the offer has no funds. */
  @Column(name = "funds_json")
  private String fundsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public IntegrationJobOfferEntity() {}

  public String getOfferId() {
    return offerId;
  }

  public void setOfferId(String offerId) {
    this.offerId = offerId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public void setInsurerCode(String insurerCode) {
    this.insurerCode = insurerCode;
  }

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public BigDecimal getPremiumAmount() {
    return premiumAmount;
  }

  public void setPremiumAmount(BigDecimal premiumAmount) {
    this.premiumAmount = premiumAmount;
  }

  public String getPremiumFrequency() {
    return premiumFrequency;
  }

  public void setPremiumFrequency(String premiumFrequency) {
    this.premiumFrequency = premiumFrequency;
  }

  public BigDecimal getSumAssured() {
    return sumAssured;
  }

  public void setSumAssured(BigDecimal sumAssured) {
    this.sumAssured = sumAssured;
  }

  public Boolean getOutOfBound() {
    return outOfBound;
  }

  public void setOutOfBound(Boolean outOfBound) {
    this.outOfBound = outOfBound;
  }

  public String getOfferStatus() {
    return offerStatus;
  }

  public void setOfferStatus(String offerStatus) {
    this.offerStatus = offerStatus;
  }

  public String getErrorSummary() {
    return errorSummary;
  }

  public void setErrorSummary(String errorSummary) {
    this.errorSummary = errorSummary;
  }

  public String getRawOfferBlobId() {
    return rawOfferBlobId;
  }

  public void setRawOfferBlobId(String rawOfferBlobId) {
    this.rawOfferBlobId = rawOfferBlobId;
  }

  public String getFundsJson() {
    return fundsJson;
  }

  public void setFundsJson(String fundsJson) {
    this.fundsJson = fundsJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
