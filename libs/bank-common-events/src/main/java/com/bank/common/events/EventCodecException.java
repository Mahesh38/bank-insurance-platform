package com.bank.common.events;

/** The JSON is not a valid {@code event-envelope/v1}; a consumer sends such a message to DLQ. */
public class EventCodecException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EventCodecException(String message, Throwable cause) {
    super(message, cause);
  }
}
