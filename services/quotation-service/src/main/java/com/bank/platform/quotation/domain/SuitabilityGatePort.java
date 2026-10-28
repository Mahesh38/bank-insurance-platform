package com.bank.platform.quotation.domain;

/**
 * C1 gate (D4 / S-08): the usable suitability evaluation for this customer, LOB and product class,
 * or refuse. Failures surface as {@code SUITABILITY_REQUIRED} or {@code UPSTREAM_UNAVAILABLE}.
 */
public interface SuitabilityGatePort {

  void requireUsable(String customerId, String lob, String productClass, String evaluationId);
}
