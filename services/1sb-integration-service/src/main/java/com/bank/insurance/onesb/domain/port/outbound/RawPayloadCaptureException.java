package com.bank.insurance.onesb.domain.port.outbound;

/**
 * Evidence of a request could not be stored, so the request was not sent (SUG-20261021-gap).
 * Callers translate it to a retryable 503: nothing reached 1SB, so a retry is safe.
 */
public class RawPayloadCaptureException extends RuntimeException {

  public RawPayloadCaptureException(String message, Throwable cause) {
    super(message, cause);
  }
}
