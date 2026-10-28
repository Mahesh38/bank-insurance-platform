package com.bank.platform.integration.hub.domain;

import java.util.Optional;

/** Persistence port for hub.provider_request (LLD §7). */
public interface ProviderRequestStore {

  Optional<ProviderRequest> findByIdempotencyKey(String idempotencyKey);

  Optional<ProviderRequest> findById(String providerRequestId);

  void save(ProviderRequest request);
}
