package com.bank.platform.quotation.domain;

/**
 * Consent checkpoint {@code BEFORE_MULTI_QUOTE} (D5). Returns the covering consent id when
 * satisfied; otherwise refuses with {@code CONSENT_REQUIRED} (nothing is written).
 */
public interface ConsentCheckpointPort {

  String requireBeforeMultiQuote(String customerId);
}
