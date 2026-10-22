package com.bank.platform.consent.domain;

/** Security event for a subject mismatch. The payload must already be free of CIF and OTP. */
public interface SecurityEventSink {

  void subjectMismatch(String actorId, String journeyId, String consentId);
}
