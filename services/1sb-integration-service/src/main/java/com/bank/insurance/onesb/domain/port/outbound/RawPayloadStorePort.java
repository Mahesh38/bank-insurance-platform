package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.common.domain.Lob;
import com.bank.common.domain.RawPayloadDirection;

/**
 * Port for capturing full 1SB request/response bodies as dispute/audit evidence (COMP-003).
 * Implemented by {@code adapter.persistence.HttpRawPayloadStoreAdapter}, which encrypts nothing
 * itself — encryption at rest happens server-side in bank-persistence-service.
 *
 * <p>Two strengths (SUG-20261021-gap):
 *
 * <ul>
 *   <li>{@link #store} is best-effort: a failure is logged and never fails the caller. Used for
 *       quote traffic and for responses — once 1SB has answered, failing the flow would only invite
 *       a duplicate submission on retry.
 *   <li>{@link #storeEvidence} must succeed: used for the REQUEST of a proposal submission or a
 *       payment-link creation, before anything is sent to 1SB. A dispute about money or an
 *       application needs the exact request; if it cannot be kept, the request is not made.
 * </ul>
 */
public interface RawPayloadStorePort {

  void store(
      String jobId,
      RawPayloadDirection direction,
      String operation,
      Lob lob,
      String payload,
      Integer httpStatus);

  /**
   * Stores the payload or throws {@link RawPayloadCaptureException}. The default delegates to
   * {@link #store} for test doubles; the production adapter overrides it.
   */
  default void storeEvidence(
      String jobId,
      RawPayloadDirection direction,
      String operation,
      Lob lob,
      String payload,
      Integer httpStatus) {
    store(jobId, direction, operation, lob, payload, httpStatus);
  }
}
