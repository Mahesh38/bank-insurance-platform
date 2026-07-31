package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;

/**
 * Inbound use-case for proposal schema retrieval (FUNC-004).
 */
public interface ProposalUseCase {

    /**
     * Returns the dynamic proposal schema for the given product keys.
     *
     * @param quoteJobId optional; when present, validates the quote job is still usable
     */
    ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                             String version, String quoteJobId);
}
