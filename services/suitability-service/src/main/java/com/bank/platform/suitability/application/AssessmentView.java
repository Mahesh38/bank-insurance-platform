package com.bank.platform.suitability.application;

import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Seller view of one assessment. The raw consent token is never included. */
public record AssessmentView(
    String assessmentId,
    String leadId,
    String journeyId,
    String customerId,
    String productType,
    String status,
    String lastScreen,
    String screenTitle,
    boolean awaitingConsent,
    String proposerName,
    ConsentPanel consent,
    ExitCopy exit,
    List<ProductView> products,
    DerivedView derived,
    String notice,
    String noProductMessage,
    Map<String, Object> answers) {

  public record ConsentPanel(
      String state,
      String title,
      String message,
      String resendLabel,
      String maskedMobile,
      String maskedEmail) {}

  public record ExitCopy(String title, String message) {}

  public record ProductView(String productId, String insurerCode, String name, String productType) {
    static ProductView from(ProductMatch match) {
      return new ProductView(
          match.productId(), match.insurerCode(), match.name(), match.productType());
    }
  }

  public record DerivedView(
      Long premiumInr,
      String frequency,
      String premiumPayingTerm,
      String policyTerm,
      String ruleId,
      Instant assessedAt) {}
}
