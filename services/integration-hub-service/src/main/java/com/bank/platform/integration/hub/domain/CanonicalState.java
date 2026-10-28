package com.bank.platform.integration.hub.domain;

/** Canonical provider-request lifecycle (Hub OpenAPI CanonicalState). */
public enum CanonicalState {
  DISPATCHING,
  IN_PROGRESS,
  SUBMISSION_UNCERTAIN,
  COMPLETED,
  PARTIALLY_COMPLETED,
  FAILED,
  TIMED_OUT
}
