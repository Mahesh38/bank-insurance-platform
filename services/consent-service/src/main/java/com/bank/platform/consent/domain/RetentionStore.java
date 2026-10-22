package com.bank.platform.consent.domain;

import java.time.Instant;

/** Insert-only retention anchor. Seven years is applied from the journey's terminal instant. */
public interface RetentionStore {

  void anchor(String rootConsentId, Instant terminalAt, Instant retainUntil);
}
