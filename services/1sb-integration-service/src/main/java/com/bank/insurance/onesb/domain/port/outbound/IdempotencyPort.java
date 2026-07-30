package com.bank.insurance.onesb.domain.port.outbound;

import java.util.Optional;

/**
 * Port for Idempotency-Key check-and-store. In-memory for Phase 2; Redis later.
 */
public interface IdempotencyPort {

    /**
     * Lookup a previously stored response for the given key.
     *
     * @return empty if key is unknown
     */
    Optional<CachedResponse> find(String idempotencyKey);

    /**
     * Store the response for a key after first successful handling.
     * Implementations may no-op if the key is already present (first-writer wins).
     */
    void store(String idempotencyKey, String bodyHash, CachedResponse response);

    /**
     * Cached HTTP response payload for idempotent replay.
     */
    record CachedResponse(
            int status,
            String contentType,
            byte[] body,
            String bodyHash
    ) {}
}
