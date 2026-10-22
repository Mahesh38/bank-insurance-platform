package com.bank.platform.consent.domain;

/** What the caller should do. This service does not call Journey or Notification itself. */
public enum JourneyEffect {
  PROCEED,
  BLOCK_STEP,
  HALT_JOURNEY,
  HALT_FURTHER_SHARING,
  SUPPRESS_OPTIONAL_COMMUNICATIONS
}
