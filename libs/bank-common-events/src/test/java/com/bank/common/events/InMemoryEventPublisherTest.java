package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InMemoryEventPublisherTest {

  @Test
  void recordsFailsOnDemandAndClears() {
    InMemoryEventPublisher publisher = new InMemoryEventPublisher();
    EventEnvelope ok = TestEvents.event(Ulid.mint(), "PaymentCaptured");
    EventEnvelope bad = TestEvents.event(Ulid.mint(), "PaymentDeclined");
    publisher.failWhen(e -> e.eventType().equals("PaymentDeclined"));

    publisher.publish(ok);
    assertThatThrownBy(() -> publisher.publish(bad)).isInstanceOf(EventPublishException.class);
    assertThat(publisher.published()).containsExactly(ok);

    publisher.clear();
    assertThat(publisher.published()).isEmpty();
    assertThat(new EventPublishException("x").getMessage()).isEqualTo("x");
  }
}
