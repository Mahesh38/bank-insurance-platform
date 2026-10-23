package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_event", schema = "consent")
public class OutboxEntity {

  @Id
  @Column(name = "outbox_id", length = 26)
  private String outboxId;

  @Column(name = "aggregate_type", nullable = false, length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 64)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 96)
  private String eventType;

  @Column(name = "schema_version", nullable = false)
  private short schemaVersion = 1;

  @Column(nullable = false, length = 8000)
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private int attempts;

  public void setOutboxId(String outboxId) {
    this.outboxId = outboxId;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public String getAggregateId() {
    return aggregateId;
  }
}
