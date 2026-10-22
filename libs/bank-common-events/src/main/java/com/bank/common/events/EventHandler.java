package com.bank.common.events;

/** A consumer's business reaction to one event. */
@FunctionalInterface
public interface EventHandler {

  void handle(EventEnvelope event);
}
