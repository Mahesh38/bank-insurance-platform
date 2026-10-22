package com.bank.platform.consent.application;

/** Facts the caller presents when asking whether a consent is enough. */
public record EvaluationCommand(
    String customerId,
    String consentCode,
    String counterpartyId,
    String currentStatementVersion,
    boolean pastBlockingPoint,
    boolean proposalAlreadySubmitted,
    String presentedCif,
    String journeyId,
    String presentedConsentId,
    String actorId) {}
