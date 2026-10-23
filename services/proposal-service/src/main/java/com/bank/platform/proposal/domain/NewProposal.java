package com.bank.platform.proposal.domain;

/** A create request: a {@code DRAFT} for the selected offer of a quote (proposal API §3.1). */
public record NewProposal(
    String leadId,
    String journeyId,
    String customerId,
    String quoteId,
    String offerId,
    String consentId,
    String lob,
    String schemaId,
    String schemaVersion,
    String issuanceMode,
    String configVersionRef) {}
