package com.bank.platform.proposal.domain;

/** The {@code IDEM#key} item a create claims (schemas/20 §3.1). */
public record IdempotencyClaim(String key, String requestHash) {}
