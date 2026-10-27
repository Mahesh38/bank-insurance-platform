package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.persistence.adapter.persistence.RawPayloadEntity;
import com.bank.persistence.adapter.persistence.RawPayloadRepository;
import com.bank.persistence.config.RawPayloadRetentionProperties;
import com.bank.persistence.crypto.RawPayloadEncryptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Encrypted store for full 1SB request/response bodies (COMP-003 — dispute/audit evidence). {@code
 * payload} is encrypted (AES-256-GCM) before it is written; only {@link #get} decrypts it back.
 * {@link #byJobId} returns metadata only, never plaintext.
 */
@RestController
@RequestMapping("/internal/v1/raw-payloads")
public class RawPayloadController {

  private final RawPayloadRepository rawPayloadRepository;
  private final RawPayloadEncryptionService encryptionService;
  private final RawPayloadRetentionProperties retentionProperties;
  private final ServiceErrors serviceErrors;
  private final PrivilegeGuard privileges;

  public RawPayloadController(
      RawPayloadRepository rawPayloadRepository,
      RawPayloadEncryptionService encryptionService,
      RawPayloadRetentionProperties retentionProperties,
      ServiceErrors serviceErrors,
      PrivilegeGuard privileges) {
    this.serviceErrors = serviceErrors;
    this.rawPayloadRepository = rawPayloadRepository;
    this.encryptionService = encryptionService;
    this.retentionProperties = retentionProperties;
    this.privileges = privileges;
  }

  @PostMapping
  public ResponseEntity<RawPayloadSummaryResponse> create(
      HttpServletRequest http, @Valid @RequestBody CreateRawPayloadRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_RAW_PAYLOAD_WRITE, "create");
    Instant now = Instant.now();
    RawPayloadEntity entity = new RawPayloadEntity();
    entity.setPayloadId(UUID.randomUUID().toString());
    entity.setJobId(request.jobId());
    entity.setDirection(request.direction());
    entity.setOperation(request.operation());
    entity.setLob(request.lob());
    entity.setPayloadEnc(encryptionService.encrypt(request.payload()));
    entity.setEncryptionKeyId(encryptionService.keyId());
    entity.setHttpStatus(request.httpStatus());
    entity.setCreatedAt(now);
    entity.setRetainUntil(
        request.retainUntil() != null
            ? request.retainUntil()
            : LocalDate.ofInstant(now, ZoneOffset.UTC)
                .plusYears(retentionProperties.yearsOrDefault()));

    RawPayloadEntity saved = rawPayloadRepository.save(entity);
    return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(saved));
  }

  @GetMapping("/{payloadId}")
  public RawPayloadResponse get(HttpServletRequest http, @PathVariable String payloadId) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_RAW_PAYLOAD_DECRYPT, "get");
    RawPayloadEntity entity =
        rawPayloadRepository
            .findById(payloadId)
            .orElseThrow(() -> NotFound.of(serviceErrors, "Raw payload", payloadId));
    return new RawPayloadResponse(
        entity.getPayloadId(),
        entity.getJobId(),
        entity.getDirection(),
        entity.getOperation(),
        entity.getLob(),
        encryptionService.decrypt(entity.getPayloadEnc()),
        entity.getHttpStatus(),
        entity.getCreatedAt(),
        entity.getRetainUntil());
  }

  @GetMapping
  public List<RawPayloadSummaryResponse> byJobId(
      HttpServletRequest http, @RequestParam String jobId) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_RAW_PAYLOAD_WRITE, "byJobId");
    return rawPayloadRepository.findByJobId(jobId).stream()
        .map(RawPayloadController::toSummary)
        .toList();
  }

  private static RawPayloadSummaryResponse toSummary(RawPayloadEntity e) {
    return new RawPayloadSummaryResponse(
        e.getPayloadId(),
        e.getJobId(),
        e.getDirection(),
        e.getOperation(),
        e.getLob(),
        e.getHttpStatus(),
        e.getCreatedAt(),
        e.getRetainUntil());
  }
}
