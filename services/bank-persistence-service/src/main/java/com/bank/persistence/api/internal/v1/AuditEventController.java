package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.persistence.adapter.persistence.AuditEventEntity;
import com.bank.persistence.adapter.persistence.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Insert-only audit log. A row is never updated: replaying an {@code eventId} is idempotent.
 *
 * <ul>
 *   <li>new {@code eventId} → INSERT, {@code 201};
 *   <li>same {@code eventId}, same content → the stored row, {@code 200}, nothing written;
 *   <li>same {@code eventId}, different content → {@code 409 IDEMPOTENCY_CONFLICT}, nothing written
 *       (evidence is never overwritten, READINESS-REVIEW §3.6).
 * </ul>
 *
 * <p>"Same content" compares every field the caller supplied. {@code eventTime} and {@code
 * eventSchemaVersion} are server-defaulted when absent, so an absent value on replay matches
 * whatever was stored. Times are compared at microsecond precision (the column's).
 *
 * <p>A (journey_id, sequence_no) pair already taken by another event is {@code 409 CONFLICT}
 * (ux_audit_journey_sequence, V4/V5).
 */
@RestController
@RequestMapping("/internal/v1/audit-events")
public class AuditEventController {

  private final AuditEventRepository auditEventRepository;
  private final ServiceErrors errors;
  private final PrivilegeGuard privileges;

  public AuditEventController(
      AuditEventRepository auditEventRepository, ServiceErrors errors, PrivilegeGuard privileges) {
    this.auditEventRepository = auditEventRepository;
    this.errors = errors;
    this.privileges = privileges;
  }

  @PostMapping
  public ResponseEntity<AuditEventResponse> append(
      HttpServletRequest http, @Valid @RequestBody CreateAuditEventRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_AUDIT_APPEND, "append");
    String eventId = request.eventId() != null ? request.eventId() : UUID.randomUUID().toString();
    Optional<AuditEventEntity> existing = auditEventRepository.findById(eventId);
    if (existing.isPresent()) {
      return replay(existing.get(), request);
    }
    AuditEventEntity entity = toEntity(eventId, request);
    try {
      AuditEventEntity saved = auditEventRepository.saveAndFlush(entity);
      return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    } catch (DataIntegrityViolationException ex) {
      // Either a concurrent insert of the same eventId won, or the sequence pair is taken.
      Optional<AuditEventEntity> raced = auditEventRepository.findById(eventId);
      if (raced.isPresent()) {
        return replay(raced.get(), request);
      }
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("AuditEventController")
          .operation("append")
          .reason(
              "audit row violates a uniqueness or check constraint"
                  + " (journey_id, sequence_no already used?)")
          .cause(ex)
          .build();
    }
  }

  @GetMapping
  public List<AuditEventResponse> byResourceId(
      HttpServletRequest http, @RequestParam String resourceId) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_AUDIT_READ, "byResourceId");
    return auditEventRepository.findByResourceIdOrderByEventTimeAsc(resourceId).stream()
        .map(AuditEventController::toResponse)
        .toList();
  }

  private ResponseEntity<AuditEventResponse> replay(
      AuditEventEntity stored, CreateAuditEventRequest request) {
    if (!sameContent(stored, request)) {
      throw errors
          .error(ErrorCodes.IDEMPOTENCY_CONFLICT)
          .component("AuditEventController")
          .operation("append")
          .reason("eventId " + stored.getEventId() + " was already recorded with different content")
          .build();
    }
    return ResponseEntity.ok(toResponse(stored));
  }

  static boolean sameContent(AuditEventEntity e, CreateAuditEventRequest r) {
    return (r.eventTime() == null
            || Objects.equals(micros(r.eventTime()), micros(e.getEventTime())))
        && (r.eventSchemaVersion() == null
            || Objects.equals(r.eventSchemaVersion(), e.getEventSchemaVersion()))
        && Objects.equals(r.action(), e.getAction())
        && Objects.equals(r.actorId(), e.getActorId())
        && Objects.equals(r.actorType(), e.getActorType())
        && Objects.equals(r.resourceType(), e.getResourceType())
        && Objects.equals(r.resourceId(), e.getResourceId())
        && Objects.equals(r.outcome(), e.getOutcome())
        && Objects.equals(r.lob(), e.getLob())
        && Objects.equals(r.journeyId(), e.getJourneyId())
        && Objects.equals(r.distributorId(), e.getDistributorId())
        && Objects.equals(r.agentId(), e.getAgentId())
        && Objects.equals(r.metadata(), e.getMetadata())
        && Objects.equals(r.traceId(), e.getTraceId())
        && Objects.equals(r.priorState(), e.getPriorState())
        && Objects.equals(r.newState(), e.getNewState())
        && Objects.equals(r.consentRef(), e.getConsentRef())
        && Objects.equals(r.suitabilityRef(), e.getSuitabilityRef())
        && Objects.equals(micros(r.retainUntil()), micros(e.getRetainUntil()))
        && Objects.equals(r.sequenceNo(), e.getSequenceNo())
        && Objects.equals(r.actingCapacity(), e.getActingCapacity())
        && Objects.equals(r.actorInsurerId(), e.getActorInsurerId())
        && Objects.equals(r.assistedActorId(), e.getAssistedActorId())
        && Objects.equals(r.configVersionRef(), e.getConfigVersionRef());
  }

  private static Instant micros(Instant at) {
    return at == null ? null : at.truncatedTo(ChronoUnit.MICROS);
  }

  static AuditEventEntity toEntity(String eventId, CreateAuditEventRequest request) {
    AuditEventEntity entity = new AuditEventEntity();
    entity.setEventId(eventId);
    entity.setEventTime(micros(request.eventTime() != null ? request.eventTime() : Instant.now()));
    entity.setAction(request.action());
    entity.setActorId(request.actorId());
    entity.setActorType(request.actorType());
    entity.setResourceType(request.resourceType());
    entity.setResourceId(request.resourceId());
    entity.setOutcome(request.outcome());
    entity.setLob(request.lob());
    entity.setJourneyId(request.journeyId());
    entity.setDistributorId(request.distributorId());
    entity.setAgentId(request.agentId());
    entity.setMetadata(request.metadata());
    entity.setTraceId(request.traceId());
    entity.setPriorState(request.priorState());
    entity.setNewState(request.newState());
    entity.setConsentRef(request.consentRef());
    entity.setSuitabilityRef(request.suitabilityRef());
    entity.setEventSchemaVersion(
        request.eventSchemaVersion() != null ? request.eventSchemaVersion() : (short) 1);
    entity.setRetainUntil(micros(request.retainUntil()));
    entity.setSequenceNo(request.sequenceNo());
    entity.setActingCapacity(request.actingCapacity());
    entity.setActorInsurerId(request.actorInsurerId());
    entity.setAssistedActorId(request.assistedActorId());
    entity.setConfigVersionRef(request.configVersionRef());
    return entity;
  }

  static AuditEventResponse toResponse(AuditEventEntity e) {
    return new AuditEventResponse(
        e.getEventId(),
        e.getEventTime(),
        e.getAction(),
        e.getActorId(),
        e.getActorType(),
        e.getResourceType(),
        e.getResourceId(),
        e.getOutcome(),
        e.getLob(),
        e.getJourneyId(),
        e.getDistributorId(),
        e.getAgentId(),
        e.getMetadata(),
        e.getTraceId(),
        e.getPriorState(),
        e.getNewState(),
        e.getConsentRef(),
        e.getSuitabilityRef(),
        e.getEventSchemaVersion(),
        e.getRetainUntil(),
        e.getSequenceNo(),
        e.getActingCapacity(),
        e.getActorInsurerId(),
        e.getAssistedActorId(),
        e.getConfigVersionRef());
  }
}
