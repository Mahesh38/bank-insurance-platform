package com.bank.insurance.onesb.adapter.idempotency;

import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 2 in-memory idempotency store (KISS). Swap for Redis via {@link IdempotencyPort}.
 */
@Component
public class InMemoryIdempotencyStore implements IdempotencyPort {

    private final ConcurrentHashMap<String, CachedResponse> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CachedResponse> find(String idempotencyKey) {
        return Optional.ofNullable(store.get(idempotencyKey));
    }

    @Override
    public void store(String idempotencyKey, String bodyHash, CachedResponse response) {
        store.putIfAbsent(idempotencyKey, response);
    }

    /** Test / admin helper — clears all entries. */
    void clear() {
        store.clear();
    }
}
