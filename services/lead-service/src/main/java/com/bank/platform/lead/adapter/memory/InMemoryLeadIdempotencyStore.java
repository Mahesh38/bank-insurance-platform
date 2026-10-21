package com.bank.platform.lead.adapter.memory;

import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.LeadIdempotencyPort;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Test and local-only companion of {@link InMemoryLeadRepository} ({@code lead.store=memory}). */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "memory")
public class InMemoryLeadIdempotencyStore implements LeadIdempotencyPort {

  private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

  @Override
  public Optional<IdempotencyRecord> find(String idempotencyKey, Instant now) {
    return Optional.ofNullable(records.get(idempotencyKey))
        .filter(record -> record.expiresAt().isAfter(now));
  }

  @Override
  public void save(IdempotencyRecord record) {
    records.compute(
        record.idempotencyKey(),
        (key, existing) -> {
          if (existing != null && existing.expiresAt().isAfter(record.createdAt())) {
            throw new ConcurrentLeadUpdateException(record.responseRef());
          }
          return record;
        });
  }
}
