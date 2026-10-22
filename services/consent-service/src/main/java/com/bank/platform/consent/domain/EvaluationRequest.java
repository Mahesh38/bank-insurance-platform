package com.bank.platform.consent.domain;

public record EvaluationRequest(
    String customerId,
    ConsentCode code,
    String counterpartyId,
    String currentStatementVersion,
    boolean pastBlockingPoint,
    boolean proposalAlreadySubmitted) {}
