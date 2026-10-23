package com.bank.platform.quotation.domain;

/**
 * Extra conditions the store adds to the optimistic-lock condition, so a rule checked on the read
 * also holds at the write (schemas/20 §3.2, §3.3).
 */
public enum StoreGuard {
  NONE,
  /** {@code state IN (QUOTED, PARTIALLY_QUOTED) AND validUntil > :now}. */
  SELECTABLE,
  /** {@code state = SELECTED}. */
  SELECTED,
  /** {@code state = SELECTED AND validUntil > :now} — a new payment hold (LLD §4.4). */
  HOLDABLE
}
