package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.common.domain.Lob;
import com.bank.common.domain.RawPayloadDirection;

/**
 * Port for capturing full 1SB request/response bodies as dispute/audit evidence (COMP-003).
 * Implemented by {@code adapter.persistence.HttpRawPayloadStoreAdapter}, which encrypts nothing
 * itself — encryption at rest happens server-side in bank-persistence-service.
 * <p>
 * Implementations must be best-effort: a failure to capture a raw payload must never fail the
 * caller's business flow (dependency-down does not crash the process, NFR-002).
 */
public interface RawPayloadStorePort {

    void store(String jobId, RawPayloadDirection direction, String operation, Lob lob,
              String payload, Integer httpStatus);
}
