package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.common.domain.Lob;

/**
 * Strategy interface for LOB-specific proposal path/payload translation.
 */
public interface LobProposalHandler {

    Lob supportedLob();

    /**
     * Relative 1SB GET path (including query string) for the dynamic proposal form schema.
     */
    String schemaPath(String productCode, String manufacturerId, String version);

    /**
     * Relative 1SB POST path for proposal submit.
     */
    String submitPath();

    /**
     * Relative 1SB GET path for proposal poll given the external request id.
     */
    String pollPath(String externalReqId);

    /**
     * Builds the outbound 1SB proposal body from bank {@link SubmitProposalCommand}.
     * Must inject distributor attribution from secrets — never trust client {@code distributorId}.
     */
    Object buildSubmitPayload(SubmitProposalCommand command);
}
