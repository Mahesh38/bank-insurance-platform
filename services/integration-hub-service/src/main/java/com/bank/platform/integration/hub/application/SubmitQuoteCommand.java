package com.bank.platform.integration.hub.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Application command for Hub QUOTE submit (FUNC-032). */
public record SubmitQuoteCommand(
    String idempotencyKey,
    String actorId,
    String correlationId,
    String callerServiceId,
    String operation,
    String lob,
    String productClass,
    CallerRefView callerRef,
    String journeyId,
    String mode,
    List<String> insurerCodes,
    String category,
    BigDecimal requestedCover,
    BigDecimal premiumAmount,
    List<Member> members,
    Preferences preferences,
    Selection selection,
    Map<String, Object> rawTreeForAttribution) {

  public record CallerRefView(String context, String aggregateId) {}

  public record Member(
      String role,
      int sequence,
      String dob,
      String gender,
      Boolean tobacco,
      BigDecimal annualIncome,
      String pincode,
      String relationship) {}

  public record Preferences(String paymentFrequency, Integer policyTerm) {}

  public record Selection(
      String insurerCode,
      List<String> productCodes,
      String planOption,
      String coverOption,
      String deathBenefitOption,
      Integer policyTerm,
      Integer premiumPaymentTerm,
      String premiumFrequency,
      String premiumPaymentOption) {}
}
