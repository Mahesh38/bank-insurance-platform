package com.bank.platform.consent.application;

public record OpenGrantCommand(
    String leadId,
    String journeyId,
    String customerId,
    String customerCif,
    String consentCode,
    String statementId,
    String statementVersion,
    String language,
    String channel,
    String registeredContactRef,
    String capturedByActorId,
    String capturedByAgentId,
    String lob,
    String counterpartyId,
    String configVersionRef,
    String sourceIp) {}
