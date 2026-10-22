package com.bank.platform.consent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Request bodies for the consent API. Unknown properties are rejected by Jackson. */
public final class ConsentRequests {

  private ConsentRequests() {}

  public record PublishStatementRequest(
      @NotBlank String statementId,
      @NotBlank String version,
      @NotBlank String text,
      @NotBlank String language,
      @NotBlank String changeClass,
      @NotBlank String classifiedBy) {}

  public record OpenGrantRequest(
      @NotBlank String leadId,
      @NotBlank String journeyId,
      @NotBlank String customerId,
      @NotBlank String customerCif,
      @NotBlank String consentCode,
      @NotBlank String statementId,
      @NotBlank String statementVersion,
      String language,
      @NotBlank String channel,
      @NotBlank String registeredContactRef,
      @NotBlank String capturedByActorId,
      String capturedByAgentId,
      @NotBlank String lob,
      String counterpartyId,
      @NotBlank String configVersionRef) {}

  public record VerifyOtpRequest(@NotBlank String otp, @NotBlank String actorRole) {}

  public record WithdrawConsentRequest(
      @NotBlank String registeredContactRef,
      @NotBlank String reasonCode,
      boolean proposalAlreadySubmitted) {}

  public record EvaluateConsentRequest(
      @NotBlank String customerId,
      @NotBlank String consentCode,
      String counterpartyId,
      String currentStatementVersion,
      boolean pastBlockingPoint,
      boolean proposalAlreadySubmitted,
      @NotBlank String customerCif,
      String journeyId,
      String presentedConsentId) {}

  public record AnchorRetentionRequest(@NotNull Instant terminalAt) {}
}
