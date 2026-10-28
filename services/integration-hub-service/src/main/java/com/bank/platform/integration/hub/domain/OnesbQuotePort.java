package com.bank.platform.integration.hub.domain;

/**
 * Outbound port to {@code 1sb-integration-service} quote APIs. Implementations live under {@code
 * adapter.onesb} and must not leak 1SB package types into Hub domain.
 */
public interface OnesbQuotePort {

  /** Outcome of a single submit attempt (no Hub retry). */
  sealed interface SubmitOutcome {
    record Accepted(String jobId) implements SubmitOutcome {}

    /** Transport timed out after the request may have been sent — never resubmit. */
    record Uncertain() implements SubmitOutcome {}
  }

  SubmitOutcome submitQuote(AdapterQuoteSubmit submit);

  AdapterQuoteJob getQuoteJob(String jobId);

  /** Adapter-facing submit body (JSON shape of 1SB {@code POST /v1/quotes}). */
  record AdapterQuoteSubmit(
      String lob,
      String mode,
      String category,
      java.math.BigDecimal sumAssured,
      java.math.BigDecimal premiumAmount,
      java.util.List<AdapterMember> members,
      java.util.Map<String, Object> preferences,
      AdapterDistribution distribution,
      String journeyId,
      AdapterSelection selection,
      String idempotencyKey,
      String actorId,
      String correlationId) {}

  record AdapterMember(
      String role,
      Integer sequenceNumber,
      String dob,
      String gender,
      Boolean tobacco,
      java.math.BigDecimal annualIncome,
      String pincode,
      String relationship) {}

  record AdapterDistribution(String rmEmployeeId, String agentId, String channelType) {}

  record AdapterSelection(
      String insurerCode,
      java.util.List<String> productCodes,
      String planOption,
      String coverOption,
      String deathBenefitOption,
      Integer policyTerm,
      Integer premiumPaymentTerm,
      String premiumFrequency,
      String premiumPaymentOption) {}

  record AdapterQuoteJob(
      String jobId, String status, String failureReason, java.util.List<AdapterOffer> offers) {}

  record AdapterOffer(
      String offerId,
      String insurerCode,
      String insurerName,
      String productCode,
      String productName,
      java.math.BigDecimal premiumAmount,
      String premiumFrequency,
      java.math.BigDecimal sumAssured,
      boolean outOfBound,
      String offerStatus,
      String errorSummary,
      Integer coverTerm,
      Integer premiumPayingTerm,
      java.util.List<CanonicalOffer.FundAllocation> funds) {}
}
