package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
import com.bank.insurance.onesb.domain.model.ProposalSchema;

/**
 * Outbound port for 1SB proposal-form (schema) retrieval and proposal submit.
 * Implemented by {@code adapter.onesb.proposal}; only that package may hold 1SB HTTP types.
 */
public interface OneSbProposalPort {

    /**
     * Fetches the dynamic proposal form schema from 1SB at the given relative path
     * (including query string). Returns a pass-through {@link ProposalSchema}.
     */
    ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                             String version, String path);

    /**
     * Submits a proposal to 1SB at the given relative path.
     * Business rejects are mapped to {@code PROPOSAL_REJECTED}.
     */
    OneSbProposalSubmitResult submit(String jobId, String path, Object payload);
}
