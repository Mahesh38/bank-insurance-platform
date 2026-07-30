package com.bank.persistence.api.internal.v1;

import com.bank.persistence.api.internal.v1.dto.AuditEventResponse;
import com.bank.persistence.api.internal.v1.dto.CreateAuditEventRequest;
import com.bank.persistence.entity.AuditEventEntity;
import com.bank.persistence.repo.AuditEventRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/audit-events")
public class AuditEventController {

    private final AuditEventRepository auditEventRepository;

    public AuditEventController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @PostMapping
    public ResponseEntity<AuditEventResponse> append(@Valid @RequestBody CreateAuditEventRequest request) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setEventId(request.eventId() != null ? request.eventId() : UUID.randomUUID().toString());
        entity.setEventTime(request.eventTime() != null ? request.eventTime() : Instant.now());
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

        AuditEventEntity saved = auditEventRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public List<AuditEventResponse> byResourceId(@RequestParam String resourceId) {
        return auditEventRepository.findByResourceIdOrderByEventTimeAsc(resourceId).stream()
                .map(AuditEventController::toResponse)
                .toList();
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
                e.getTraceId()
        );
    }
}
