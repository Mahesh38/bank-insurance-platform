package com.bank.platform.integration.hub.adapter.persistence;

import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for unit tests that construct the store directly. Spring context uses {@link
 * JdbcProviderRequestStore} (no {@code @Component} here — avoids a second {@link
 * ProviderRequestStore} bean).
 */
public class InMemoryProviderRequestStore implements ProviderRequestStore {

  private final ConcurrentHashMap<String, ProviderRequest> byId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> idempotencyToId = new ConcurrentHashMap<>();

  @Override
  public Optional<ProviderRequest> findByIdempotencyKey(String idempotencyKey) {
    String id = idempotencyToId.get(idempotencyKey);
    return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
  }

  @Override
  public Optional<ProviderRequest> findById(String providerRequestId) {
    return Optional.ofNullable(byId.get(providerRequestId));
  }

  @Override
  public void save(ProviderRequest request) {
    byId.put(request.providerRequestId(), request);
    idempotencyToId.put(request.idempotencyKey(), request.providerRequestId());
  }
}
