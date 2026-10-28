package com.bank.platform.quotation.domain;

import java.math.BigDecimal;
import java.util.List;

/** What a create request carries. Suitability and consent are checked before this exists (C1). */
public record NewQuote(
    String leadId,
    String journeyId,
    String customerId,
    String suitabilityId,
    String consentId,
    String lob,
    String productClass,
    String mode,
    String category,
    BigDecimal requestedCover,
    List<QuoteMember> members,
    String configVersionRef,
    BigDecimal premiumAmount,
    Preferences preferences) {

  public NewQuote {
    members = members == null ? List.of() : List.copyOf(members);
  }

  /**
   * Bank SSOT preference names (I3 / FUNC-035): premiumFrequency, policyTerm, premiumPayingTerm.
   */
  public record Preferences(
      String premiumFrequency, Integer policyTerm, Integer premiumPayingTerm) {}
}
