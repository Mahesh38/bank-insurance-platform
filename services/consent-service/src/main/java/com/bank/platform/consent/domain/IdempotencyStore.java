package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

  Optional<StoredResponse> find(String key);

  void save(String key, String requestHash, int status, String body, Instant expiresAt);

  record StoredResponse(String requestHash, int status, String body) {}
}
