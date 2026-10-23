package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.IdempotencyStore;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaIdempotencyStore implements IdempotencyStore {

  private final IdempotencyRepository repository;
  private final Clock clock;

  public JpaIdempotencyStore(IdempotencyRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public Optional<StoredResponse> find(String key) {
    return repository
        .findById(key)
        .map(
            row ->
                new StoredResponse(
                    row.getRequestHash(), row.getHttpStatus(), row.getResponseBody()));
  }

  @Override
  public void save(String key, String requestHash, int status, String body, Instant expiresAt) {
    IdempotencyEntity entity = new IdempotencyEntity();
    entity.setKey(key);
    entity.setRequestHash(requestHash);
    entity.setHttpStatus(status);
    entity.setResponseBody(body);
    entity.setCreatedAt(clock.instant());
    entity.setExpiresAt(expiresAt);
    repository.save(entity);
  }
}
