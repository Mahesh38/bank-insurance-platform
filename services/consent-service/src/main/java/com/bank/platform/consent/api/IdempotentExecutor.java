package com.bank.platform.consent.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.IdempotencyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Same key and same body replays the stored response. A different body conflicts. */
@Component
public class IdempotentExecutor {

  private static final Duration TTL = Duration.ofHours(24);

  private final IdempotencyStore store;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final ServiceErrors errors;

  public IdempotentExecutor(
      IdempotencyStore store, ObjectMapper mapper, Clock clock, ServiceErrors errors) {
    this.store = store;
    this.mapper = mapper;
    this.clock = clock;
    this.errors = errors;
  }

  @Transactional
  public ResponseEntity<String> execute(
      String key, Object request, Supplier<ResponseEntity<?>> work) {
    if (key == null || key.isBlank()) {
      throw errors.error(ErrorCodes.MISSING_IDEMPOTENCY_KEY).operation("idempotentWrite").build();
    }
    if (key.length() > 200) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "idempotency key is too long");
    }
    String hash = sha256(canonical(request));
    var existing = store.find(key);
    if (existing.isPresent()) {
      if (!existing.get().requestHash().equals(hash)) {
        throw errors.error(ErrorCodes.IDEMPOTENCY_CONFLICT).operation("idempotentWrite").build();
      }
      return json(existing.get().status(), existing.get().body());
    }
    ResponseEntity<?> response = work.get();
    String body = write(response.getBody());
    int status = response.getStatusCode().value();
    Instant expiresAt = clock.instant().plus(TTL);
    store.save(key, hash, status, body, expiresAt);
    return json(status, body);
  }

  private String canonical(Object request) {
    try {
      return mapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "request could not be read");
    }
  }

  private String write(Object body) {
    if (body == null) {
      return "{}";
    }
    if (body instanceof String text) {
      return text;
    }
    try {
      return mapper.writeValueAsString(body);
    } catch (JsonProcessingException ex) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "response could not be stored");
    }
  }

  private static ResponseEntity<String> json(int status, String body) {
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 is required");
    }
  }
}
