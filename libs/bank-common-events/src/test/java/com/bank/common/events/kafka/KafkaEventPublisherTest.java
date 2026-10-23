package com.bank.common.events.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublishException;
import com.bank.common.events.TestEvents;
import com.bank.common.events.TopicResolver;
import com.bank.common.events.Ulid;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

class KafkaEventPublisherTest {

  private final EventJsonCodec codec = new EventJsonCodec();

  @Test
  void sendsToTheAggregateTopicKeyedByAggregateId() {
    MockProducer<String, String> producer =
        new MockProducer<>(true, new StringSerializer(), new StringSerializer());
    KafkaEventPublisher publisher = publisher(producer);
    EventEnvelope event = TestEvents.paymentReconciled(Ulid.mint());

    publisher.publish(event);

    ProducerRecord<String, String> sent = producer.history().get(0);
    assertThat(sent.topic()).isEqualTo("prod.payment.payment.v1");
    assertThat(sent.key()).isEqualTo(event.aggregateId());
    assertThat(codec.decode(sent.value())).isEqualTo(event);
    assertThat(new String(sent.headers().lastHeader("ce_id").value(), StandardCharsets.UTF_8))
        .isEqualTo(event.eventId());
    assertThat(new String(sent.headers().lastHeader("ce_type").value(), StandardCharsets.UTF_8))
        .isEqualTo("PaymentReconciled");
  }

  @Test
  void brokerErrorBecomesPublishException() {
    MockProducer<String, String> producer =
        new MockProducer<>(false, new StringSerializer(), new StringSerializer());
    KafkaEventPublisher publisher = publisher(producer);
    Thread failer =
        new Thread(
            () -> {
              while (producer.history().isEmpty()) {
                Thread.onSpinWait();
              }
              producer.errorNext(new RuntimeException("NOT_ENOUGH_REPLICAS"));
            });
    failer.start();

    assertThatThrownBy(() -> publisher.publish(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
        .isInstanceOf(EventPublishException.class)
        .hasMessageContaining("NOT_ENOUGH_REPLICAS");
  }

  @Test
  void missingAckTimesOut() {
    MockProducer<String, String> producer =
        new MockProducer<>(false, new StringSerializer(), new StringSerializer());
    KafkaEventPublisher publisher =
        new KafkaEventPublisher(producer, new TopicResolver("prod"), codec, Duration.ofMillis(20));

    assertThatThrownBy(() -> publisher.publish(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
        .isInstanceOf(EventPublishException.class);
  }

  @Test
  void interruptIsPreserved() {
    MockProducer<String, String> producer =
        new MockProducer<>(false, new StringSerializer(), new StringSerializer());
    KafkaEventPublisher publisher = publisher(producer);
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> publisher.publish(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
          .isInstanceOf(EventPublishException.class)
          .hasMessageContaining("interrupted");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void producerPropertiesEnforceAcksAllAndIdempotence() {
    Properties p = KafkaEventPublisher.producerProperties("b:9098");
    assertThat(p).containsEntry(ProducerConfig.ACKS_CONFIG, "all");
    assertThat(p).containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    Properties iam = KafkaEventPublisher.mskIamProducerProperties("b:9098");
    assertThat(iam).containsEntry("sasl.mechanism", "AWS_MSK_IAM");
    assertThat(iam).containsEntry(ProducerConfig.ACKS_CONFIG, "all");
  }

  private KafkaEventPublisher publisher(MockProducer<String, String> producer) {
    return new KafkaEventPublisher(
        producer, new TopicResolver("prod"), codec, Duration.ofSeconds(5));
  }
}
