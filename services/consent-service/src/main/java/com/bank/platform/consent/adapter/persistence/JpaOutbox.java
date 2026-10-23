package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentPorts;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaOutbox implements ConsentPorts.Outbox {

  private final EntityManager entities;
  private final OutboxRepository repository;

  public JpaOutbox(EntityManager entities, OutboxRepository repository) {
    this.entities = entities;
    this.repository = repository;
  }

  @Override
  public void append(
      String eventId,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant at) {
    OutboxEntity entity = new OutboxEntity();
    entity.setOutboxId(eventId);
    entity.setAggregateType(aggregateType);
    entity.setAggregateId(aggregateId);
    entity.setEventType(eventType);
    entity.setPayload(payload);
    entity.setCreatedAt(at);
    entities.persist(entity);
  }

  public List<OutboxEntity> forAggregate(String aggregateId) {
    return repository.findByAggregateIdOrderByCreatedAtAsc(aggregateId);
  }
}
