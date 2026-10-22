package com.bank.common.events.jdbc;

/** A database failure in the outbox or the processed-event store. */
public class OutboxException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public OutboxException(String message, Throwable cause) {
    super(message, cause);
  }
}
