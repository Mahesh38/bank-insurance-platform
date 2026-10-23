package com.bank.common.events.kafka;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublishException;
import com.bank.common.events.EventPublisher;
import com.bank.common.events.TopicResolver;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * MSK adapter for {@link EventPublisher} (ADR-025 §2, §5). Plain {@code kafka-clients}, declared
 * {@code compileOnly} by this library — a service opts in by adding {@code
 * org.apache.kafka:kafka-clients} (and, for IAM auth, {@code software.amazon.msk:aws-msk-iam-auth})
 * to its own dependencies.
 *
 * <p>Topic from {@link TopicResolver}; key = {@code aggregateId} (ordering per aggregate); value =
 * the envelope JSON (CloudEvents structured mode); {@code ce_id} / {@code ce_type} headers let
 * brokers-side tooling filter without parsing. Waits for the broker ack so the relay marks a row
 * published only when MSK holds it.
 */
public final class KafkaEventPublisher implements EventPublisher {

  private final Producer<String, String> producer;
  private final TopicResolver topics;
  private final EventJsonCodec codec;
  private final Duration ackTimeout;

  public KafkaEventPublisher(
      Producer<String, String> producer,
      TopicResolver topics,
      EventJsonCodec codec,
      Duration ackTimeout) {
    this.producer = Objects.requireNonNull(producer);
    this.topics = Objects.requireNonNull(topics);
    this.codec = Objects.requireNonNull(codec);
    this.ackTimeout = Objects.requireNonNull(ackTimeout);
  }

  @Override
  public void publish(EventEnvelope event) {
    ProducerRecord<String, String> record =
        new ProducerRecord<>(topics.topicFor(event), event.aggregateId(), codec.encode(event));
    record.headers().add("ce_id", event.eventId().getBytes(StandardCharsets.UTF_8));
    record.headers().add("ce_type", event.eventType().getBytes(StandardCharsets.UTF_8));
    record
        .headers()
        .add(
            "ce_schemaversion",
            Integer.toString(event.schemaVersion()).getBytes(StandardCharsets.UTF_8));
    try {
      producer.send(record).get(ackTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new EventPublishException("interrupted publishing " + event.eventId(), ex);
    } catch (ExecutionException | TimeoutException | RuntimeException ex) {
      Throwable cause =
          ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
      throw new EventPublishException(
          "MSK did not ack " + event.eventId() + ": " + cause.getMessage(), cause);
    }
  }

  /**
   * Producer settings required by ADR-025: {@code acks=all} (with the topic's {@code
   * min.insync.replicas=2}) and idempotence on, string key/value.
   */
  public static Properties producerProperties(String bootstrapServers) {
    Properties p = new Properties();
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    p.put(ProducerConfig.ACKS_CONFIG, "all");
    p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return p;
  }

  /**
   * {@link #producerProperties} plus MSK IAM authentication. The callback handler class comes from
   * {@code aws-msk-iam-auth}, which the service must have on its runtime classpath.
   */
  public static Properties mskIamProducerProperties(String bootstrapServers) {
    Properties p = producerProperties(bootstrapServers);
    p.put("security.protocol", "SASL_SSL");
    p.put("sasl.mechanism", "AWS_MSK_IAM");
    p.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
    p.put(
        "sasl.client.callback.handler.class",
        "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
    return p;
  }
}
