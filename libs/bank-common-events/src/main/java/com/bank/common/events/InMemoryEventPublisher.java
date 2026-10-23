package com.bank.common.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Test transport (ADR-025 §5): records what was published; can be told to fail. */
public final class InMemoryEventPublisher implements EventPublisher {

  private final List<EventEnvelope> published = new ArrayList<>();
  private Predicate<EventEnvelope> failWhen = e -> false;

  @Override
  public synchronized void publish(EventEnvelope event) {
    if (failWhen.test(event)) {
      throw new EventPublishException("in-memory transport refused " + event.eventId());
    }
    published.add(event);
  }

  /** Events matching the predicate are refused until {@link #failWhen} is called again. */
  public synchronized void failWhen(Predicate<EventEnvelope> predicate) {
    this.failWhen = predicate;
  }

  public synchronized List<EventEnvelope> published() {
    return List.copyOf(published);
  }

  public synchronized void clear() {
    published.clear();
  }
}
