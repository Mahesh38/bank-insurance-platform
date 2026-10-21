package com.bank.platform.proposal.domain;

/** Extra conditions the store adds to the optimistic lock (schemas/20 §3.2, §3.3). */
public enum StoreGuard {
  NONE,
  /** {@code state = AWAITING_PAYMENT}: payment in flight / ended. */
  AWAITING_PAYMENT
}
