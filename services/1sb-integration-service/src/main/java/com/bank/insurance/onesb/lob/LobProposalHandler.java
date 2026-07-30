package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Strategy interface for LOB-specific proposal path/payload translation.
 */
public interface LobProposalHandler {

    Lob supportedLob();

    /**
     * Relative 1SB GET path (including query string) for the dynamic proposal form schema.
     */
    String schemaPath(String productCode, String manufacturerId, String version);
}
