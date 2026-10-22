package com.bank.platform.consent.domain;

/**
 * Wire codes the consent module refuses with. Platform codes are reused where they already exist.
 */
public final class ConsentCodes {

  public static final String INVALID_REQUEST = "INVALID_REQUEST";
  public static final String CONSENT_REQUIRED = "CONSENT_REQUIRED";
  public static final String CONSENT_EXPIRED = "CONSENT_EXPIRED";
  public static final String CONSENT_SCOPE_MISMATCH = "CONSENT_SCOPE_MISMATCH";
  public static final String CONSENT_SUBJECT_MISMATCH = "CONSENT_SUBJECT_MISMATCH";
  public static final String CONSENT_OTP_REQUIRED = "CONSENT_OTP_REQUIRED";
  public static final String CONSENT_OTP_NOT_VERIFIED = "CONSENT_OTP_NOT_VERIFIED";
  public static final String OTP_EXPIRED = "OTP_EXPIRED";
  public static final String OTP_ATTEMPTS_EXCEEDED = "OTP_ATTEMPTS_EXCEEDED";
  public static final String OTP_ISSUE_RATE_EXCEEDED = "OTP_ISSUE_RATE_EXCEEDED";
  public static final String CONSENT_EVIDENCE_WRITE_FAILED = "CONSENT_EVIDENCE_WRITE_FAILED";
  public static final String STATEMENT_VERSION_IMMUTABLE = "STATEMENT_VERSION_IMMUTABLE";
  public static final String JOURNEY_HALTED_CONSENT_WITHDRAWN = "JOURNEY_HALTED_CONSENT_WITHDRAWN";
  public static final String CONSENT_DEVICE_ISOLATION = "CONSENT_DEVICE_ISOLATION";
  public static final String STATEMENT_NOT_PUBLISHED = "STATEMENT_NOT_PUBLISHED";
  public static final String ILLEGAL_TRANSITION = "ILLEGAL_TRANSITION";
  public static final String CONFLICT = "CONFLICT";
  public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
  public static final String UNSUPPORTED_LOB = "UNSUPPORTED_LOB";
  public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
  public static final String MISSING_IDEMPOTENCY_KEY = "MISSING_IDEMPOTENCY_KEY";

  private ConsentCodes() {}
}
