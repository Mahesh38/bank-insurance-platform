package com.bank.platform.consent.adapter.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Constant trace for a dispatch. This type never receives the code or the contact. */
final class DispatchTrace {

  private static final Logger log = LoggerFactory.getLogger(DispatchTrace.class);

  private DispatchTrace() {}

  static void accepted() {
    log.info("consent code dispatch accepted");
  }
}
