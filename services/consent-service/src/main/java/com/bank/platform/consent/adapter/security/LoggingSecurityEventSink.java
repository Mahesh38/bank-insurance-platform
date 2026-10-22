package com.bank.platform.consent.adapter.security;

import com.bank.platform.consent.domain.SecurityEventSink;
import com.bank.platform.consent.domain.SensitiveText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Subject-mismatch security event. CIF, OTP and contact stay out of the line. */
public final class LoggingSecurityEventSink implements SecurityEventSink {

  private static final Logger log = LoggerFactory.getLogger(LoggingSecurityEventSink.class);

  @Override
  public void subjectMismatch(String actorId, String journeyId, String consentId) {
    log.warn(
        "security_event=CONSENT_SUBJECT_MISMATCH actor={} journey={} consent={}",
        SensitiveText.scrub(actorId),
        SensitiveText.scrub(journeyId),
        consentId);
  }
}
