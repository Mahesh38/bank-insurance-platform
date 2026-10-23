package com.bank.platform.quotation.application;

/** What a {@code PaymentReconciled} delivery did. Both are an ack. */
public enum MaterializeOutcome {
  MATERIALIZED,
  ALREADY_MATERIALIZED
}
