package com.bank.platform.quotation.config;

import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublisher;
import com.bank.common.events.InMemoryEventPublisher;
import com.bank.common.events.TopicResolver;
import com.bank.common.events.kafka.KafkaEventPublisher;
import com.bank.platform.quotation.application.OutboxSweeper;
import com.bank.platform.quotation.domain.OutboxStore;
import java.time.Clock;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * The {@link EventPublisher} port (ADR-025 §5) and the outbox sweeper (§3). With {@code publisher =
 * none} no publisher exists and OUTBOX items simply wait, unpublished and without {@code ttl}.
 * Kafka is wired only when configured; the in-memory transport is refused outside {@code local} /
 * {@code test}, so events can never be silently dropped in a real environment.
 */
@Configuration
@EnableScheduling
public class OutboxConfig {

  private static final Profiles LOCAL_OR_TEST = Profiles.of("local", "test");

  @Bean
  @ConditionalOnProperty(name = "quotation.outbox.publisher", havingValue = "in-memory")
  InMemoryEventPublisher inMemoryEventPublisher(Environment environment) {
    if (!environment.acceptsProfiles(LOCAL_OR_TEST)) {
      throw new IllegalStateException(
          "quotation.outbox.publisher=in-memory is only allowed in the local or test profile");
    }
    return new InMemoryEventPublisher();
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "quotation.outbox.publisher", havingValue = "kafka")
  Producer<String, String> outboxKafkaProducer(OutboxProperties p) {
    if (p.kafka().bootstrapServers() == null || p.kafka().bootstrapServers().isBlank()) {
      throw new IllegalStateException("quotation.outbox.kafka.bootstrap-servers is required");
    }
    return new KafkaProducer<>(
        KafkaEventPublisher.producerProperties(p.kafka().bootstrapServers()));
  }

  @Bean
  @ConditionalOnProperty(name = "quotation.outbox.publisher", havingValue = "kafka")
  KafkaEventPublisher kafkaEventPublisher(OutboxProperties p, Producer<String, String> producer) {
    return new KafkaEventPublisher(
        producer, new TopicResolver(p.kafka().env()), new EventJsonCodec(), p.kafka().ackTimeout());
  }

  @Bean
  @ConditionalOnExpression("'${quotation.outbox.publisher:none}' != 'none'")
  OutboxSweeper outboxSweeper(
      OutboxStore outbox, EventPublisher publisher, Clock clock, OutboxProperties p) {
    return new OutboxSweeper(
        outbox, publisher, clock, p.sweeper().minAge(), p.sweeper().batchSize());
  }

  /** Runs the sweeper on its interval; off unless {@code quotation.outbox.sweeper.enabled}. */
  @Configuration
  @ConditionalOnProperty(name = "quotation.outbox.sweeper.enabled", havingValue = "true")
  static class SweeperSchedule {

    private final OutboxSweeper sweeper;

    SweeperSchedule(OutboxSweeper sweeper) {
      this.sweeper = sweeper;
    }

    @Scheduled(
        fixedDelayString = "${quotation.outbox.sweeper.interval:PT1M}",
        initialDelayString = "${quotation.outbox.sweeper.interval:PT1M}")
    void sweep() {
      sweeper.sweep();
    }
  }
}
