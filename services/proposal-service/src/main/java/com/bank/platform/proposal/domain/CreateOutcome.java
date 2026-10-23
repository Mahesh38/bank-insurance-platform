package com.bank.platform.proposal.domain;

/** A new proposal, or the proposal an earlier identical request already created. */
public record CreateOutcome(String proposalId, boolean replayed) {}
