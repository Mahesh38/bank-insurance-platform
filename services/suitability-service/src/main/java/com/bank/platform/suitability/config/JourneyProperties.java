package com.bank.platform.suitability.config;

import com.bank.platform.suitability.domain.JourneyModel.IncomeBand;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "suitability.journey")
public class JourneyProperties {

  private String fieldKey;
  private String keyId;
  private int lockMinutes = 15;
  private String consentBaseUrl = "http://localhost:8092";
  private String consentInternalKey;
  private List<String> occupations = new ArrayList<>();
  private List<String> educations = new ArrayList<>();
  private List<Band> incomeBands = new ArrayList<>();
  private String derivationRuleId = "INTERIM-BRD-22";
  private String derivationFrequency = "ANNUAL";
  private String derivationPremiumPayingTerm = "10";
  private String derivationPolicyTerm = "20";
  private int derivationPremiumPercent = 10;

  public String getFieldKey() {
    return fieldKey;
  }

  public void setFieldKey(String fieldKey) {
    this.fieldKey = fieldKey;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public int getLockMinutes() {
    return lockMinutes;
  }

  public void setLockMinutes(int lockMinutes) {
    this.lockMinutes = lockMinutes;
  }

  public String getConsentBaseUrl() {
    return consentBaseUrl;
  }

  public void setConsentBaseUrl(String consentBaseUrl) {
    this.consentBaseUrl = consentBaseUrl;
  }

  public String getConsentInternalKey() {
    return consentInternalKey;
  }

  public void setConsentInternalKey(String consentInternalKey) {
    this.consentInternalKey = consentInternalKey;
  }

  public List<String> getOccupations() {
    return occupations;
  }

  public void setOccupations(List<String> occupations) {
    this.occupations = occupations;
  }

  public List<String> getEducations() {
    return educations;
  }

  public void setEducations(List<String> educations) {
    this.educations = educations;
  }

  public List<Band> getIncomeBands() {
    return incomeBands;
  }

  public void setIncomeBands(List<Band> incomeBands) {
    this.incomeBands = incomeBands;
  }

  public String getDerivationRuleId() {
    return derivationRuleId;
  }

  public void setDerivationRuleId(String derivationRuleId) {
    this.derivationRuleId = derivationRuleId;
  }

  public String getDerivationFrequency() {
    return derivationFrequency;
  }

  public void setDerivationFrequency(String derivationFrequency) {
    this.derivationFrequency = derivationFrequency;
  }

  public String getDerivationPremiumPayingTerm() {
    return derivationPremiumPayingTerm;
  }

  public void setDerivationPremiumPayingTerm(String derivationPremiumPayingTerm) {
    this.derivationPremiumPayingTerm = derivationPremiumPayingTerm;
  }

  public String getDerivationPolicyTerm() {
    return derivationPolicyTerm;
  }

  public void setDerivationPolicyTerm(String derivationPolicyTerm) {
    this.derivationPolicyTerm = derivationPolicyTerm;
  }

  public int getDerivationPremiumPercent() {
    return derivationPremiumPercent;
  }

  public void setDerivationPremiumPercent(int derivationPremiumPercent) {
    this.derivationPremiumPercent = derivationPremiumPercent;
  }

  public List<IncomeBand> bands() {
    List<IncomeBand> values = new ArrayList<>();
    for (Band band : incomeBands) {
      values.add(new IncomeBand(band.getLabel(), band.getFloorInr()));
    }
    return values;
  }

  public static class Band {
    private String label;
    private long floorInr;

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public long getFloorInr() {
      return floorInr;
    }

    public void setFloorInr(long floorInr) {
      this.floorInr = floorInr;
    }
  }
}
