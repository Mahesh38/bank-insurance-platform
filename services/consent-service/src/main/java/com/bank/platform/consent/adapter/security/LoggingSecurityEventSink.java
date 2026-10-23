package com.bank.platform.consent.adapter.security;

import com.bank.platform.consent.domain.SecurityEventSink;
import com.bank.platform.consent.domain.SensitiveText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Subject-mismatch security event. CIF, OTP and contact stay out of the line. */
public final class LoggingSecurityEventSink implements SecurityEventSink {

  private static final Logger log = LoggerFactory.getLogger(LoggingSecurityEventSink.class);

  @Override
  public void subjectMismatch(
      String actorId,
      String journeyId,
      String consentId,
      String requestedSubject,
      String recordedSubject) {
    log.warn(
        "security_event=CONSENT_SUBJECT_MISMATCH actor={} journey={} consent={}"
            + " requestedSubject={} recordedSubject={}",
        oneLine(actorId),
        oneLine(journeyId),
        oneLine(consentId),
        oneLine(requestedSubject),
        oneLine(recordedSubject));
  }

  /** Drops line breaks so a caller cannot forge a second log record. */
  private static String oneLine(String value) {
    String scrubbed = SensitiveText.scrub(value == null ? "" : value);
    if (scrubbed == null || scrubbed.isBlank()) {
      return "-";
    }
    return scrubbed.replace("\r", "").replace("\n", "");
  }
}
