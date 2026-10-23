package com.bank.platform.consent.domain;

/**
 * Security event for a subject mismatch (CNS-R38). Arguments are actor, journey, consent and the
 * two subject ULIDs. CIF, OTP and contact do not belong on this event.
 */
public interface SecurityEventSink {

  void subjectMismatch(
      String actorId,
      String journeyId,
      String consentId,
      String requestedSubject,
      String recordedSubject);
}
