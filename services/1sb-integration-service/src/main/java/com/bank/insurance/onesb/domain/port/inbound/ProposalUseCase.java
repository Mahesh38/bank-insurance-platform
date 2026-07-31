package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.ProposalSubmitResult;

/**
 * Inbound use-case for proposal schema retrieval (FUNC-004) and submit (FUNC-005).
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
}
