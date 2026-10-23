package com.bank.platform.adminconfig.domain;

/** ADR-024 section 5 lifecycle. {@code PUBLISHED} is terminal and immutable (SUIT-R07). */
public enum RuleSetVersionStatus {
  DRAFT,
  PENDING_APPROVAL,
  PUBLISHED,
  REJECTED,
  RETIRED
}
