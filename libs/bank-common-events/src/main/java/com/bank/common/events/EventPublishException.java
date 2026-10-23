package com.bank.common.events;

/** The transport did not accept the event; the relay counts an attempt and retries. */
public class EventPublishException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EventPublishException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventPublishException(String message) {
    super(message);
  }
}
