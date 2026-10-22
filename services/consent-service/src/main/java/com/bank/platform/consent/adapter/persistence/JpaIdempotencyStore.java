package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.IdempotencyStore;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaIdempotencyStore implements IdempotencyStore {

  private final EntityManager em;
  private final Clock clock;

  public JpaIdempotencyStore(EntityManager em, Clock clock) {
    this.em = em;
    this.clock = clock;
  }

  @Override
  public Optional<StoredResponse> find(String key) {
    IdempotencyEntity entity = em.find(IdempotencyEntity.class, key);
    if (entity == null || !entity.expiresAt().isAfter(clock.instant())) {
      return Optional.empty();
    }
    return Optional.of(
        new StoredResponse(entity.requestHash(), entity.httpStatus(), entity.responseBody()));
  }

  @Override
  public void save(String key, String requestHash, int status, String body, Instant expiresAt) {
    IdempotencyEntity entity = em.find(IdempotencyEntity.class, key);
    if (entity == null) {
      em.persist(new IdempotencyEntity(key, requestHash, status, body, expiresAt));
      return;
    }
    if (!entity.expiresAt().isAfter(clock.instant())) {
      entity.replace(requestHash, status, body, expiresAt);
    }
  }
}
