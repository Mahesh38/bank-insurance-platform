package com.bank.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventEntity implements Persistable<String> {

  @Id
  @Column(name = "event_id", length = 36, nullable = false)
  private String eventId;

  @Column(name = "event_time", nullable = false)
  private Instant eventTime;

  @Column(name = "action", length = 100, nullable = false)
  private String action;

  @Column(name = "actor_id", length = 100, nullable = false)
  private String actorId;

  @Column(name = "actor_type", length = 20, nullable = false)
  private String actorType;

  @Column(name = "resource_type", length = 50, nullable = false)
  private String resourceType;

  @Column(name = "resource_id", length = 100, nullable = false)
  private String resourceId;

  @Column(name = "outcome", length = 20, nullable = false)
  private String outcome;

  @Column(name = "lob", length = 20)
  private String lob;

  @Column(name = "journey_id", length = 36)
  private String journeyId;

  @Column(name = "distributor_id", length = 50)
  private String distributorId;

  @Column(name = "agent_id", length = 50)
  private String agentId;

  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "trace_id", length = 64)
  private String traceId;

  // V3 reconstruction columns (CR-010 DB-C1, schemas/14-audit_event_delta.sql). prior_state /
  // new_state are JSON text: TEXT, not JSONB, while this module tests on H2.
  @Column(name = "prior_state", columnDefinition = "TEXT")
  private String priorState;

  @Column(name = "new_state", columnDefinition = "TEXT")
  private String newState;

  @Column(name = "consent_ref", length = 36)
  private String consentRef;

  @Column(name = "suitability_ref", length = 36)
  private String suitabilityRef;

  @Column(name = "event_schema_version", nullable = false)
  private Short eventSchemaVersion;

  @Column(name = "retain_until")
  private Instant retainUntil;

  @Column(name = "sequence_no")
  private Integer sequenceNo;

  /** INV-ACT-04: {@code SP_ACCOUNTABLE} or {@code ASSIST_ONLY} (ck_audit_capacity, V4). */
  @Column(name = "acting_capacity", length = 20)
  private String actingCapacity;

  @Column(name = "actor_insurer_id", length = 64)
  private String actorInsurerId;

  @Column(name = "assisted_actor_id", length = 64)
  private String assistedActorId;

  @Column(name = "config_version_ref", length = 64)
  private String configVersionRef;

  @Override
  public String getId() {
    return eventId;
  }

  /**
   * Always new: an audit row is inserted once and never updated, so {@code save} must be a {@code
   * persist} (INSERT), never a {@code merge} that would turn a replay into an UPDATE of evidence
   * (READINESS-REVIEW §3.6).
   */
  @Override
  public boolean isNew() {
    return true;
  }
}
