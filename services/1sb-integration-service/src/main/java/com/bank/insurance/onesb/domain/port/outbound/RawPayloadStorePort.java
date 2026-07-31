package com.bank.insurance.onesb.domain.port.outbound;

/**
 * Port for encrypted raw 1SB REQ/RES payload persistence via bank-persistence-service.
 */
public interface RawPayloadStorePort {

    /**
     * Stores plaintext bytes (encrypted at rest by persistence). Returns payload id.
     *
     * @param direction {@code REQ} or {@code RES}
     * @param httpStatus optional upstream HTTP status (typically for RES)
     */
    String store(
            String jobId,
            String direction,
            String operation,
            String lob,
            byte[] plaintextBytes,
            Short httpStatus);
}
