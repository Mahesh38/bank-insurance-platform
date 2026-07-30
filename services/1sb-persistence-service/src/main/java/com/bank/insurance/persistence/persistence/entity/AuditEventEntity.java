package com.bank.insurance.persistence.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventEntity {

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
}
