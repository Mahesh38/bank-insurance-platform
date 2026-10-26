package com.bank.workforce.bff.application;

/**
 * The caller presented no session handle, or one that is unknown, expired or logged out. Mapped to
 * {@code SESSION_INVALID} (401), the same answer the security entry point gives on protected routes.
 */
public class SessionInvalidException extends RuntimeException {

  public SessionInvalidException(String message) {
    super(message);
  }
}
