package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Strategy interface for LOB-specific quote request translation.
 * Each implementation maps a bank-canonical {@link CreateQuoteCommand} to the
 * insurer-specific JSON payload and knows the 1SB submit/poll paths for its LOB.
 */
public interface LobQuoteHandler {

    Lob supportedLob();

    /**
     * Builds a JSON-serializable Map (or nested Maps/Lists) for the 1SB quote POST body.
     * Must not leak bank-only fields; adapter posts this as-is.
     */
    Object buildSubmitPayload(CreateQuoteCommand command);

    /** Relative 1SB path for quote submit (e.g. {@code /insurance/lifeterm/v1/quote}). */
    String submitPath();

    /**
     * Relative 1SB path for quote poll by external request id.
     * Term: {@code GET /insurance/lifeterm/v1/quote/poll/{reqId}}.
     */
    String pollPath(String externalReqId);
}
