package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.common.domain.Lob;
import com.bank.common.domain.ProposalSchema;
import com.bank.common.domain.ProposalSubmitResult;
import com.bank.common.domain.QuoteJob;

/**
 * Inbound use-case for proposal schema (FUNC-004), submit (FUNC-005), and job poll (FUNC-006).
 */
public interface ProposalUseCase {

    /**
     * Returns the dynamic proposal schema for the given product keys.
     *
     * @param quoteJobId optional; when present, validates the quote job is still usable
     */
    ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                             String version, String quoteJobId);

    /**
     * Submits a Term (or LOB) proposal: agent attribution → job → 1SB submit.
     */
    ProposalSubmitResult submit(SubmitProposalCommand command);

    /**
     * Loads a proposal job for {@code GET /v1/proposals/{jobId}}.
     * Unknown id → 404 {@code RESOURCE_NOT_FOUND}.
     */
    QuoteJob getProposalResult(String jobId);
}
