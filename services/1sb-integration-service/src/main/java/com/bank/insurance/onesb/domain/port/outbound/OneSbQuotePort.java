package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.QuoteOffer;

import java.util.List;

/**
 * Outbound port for submitting quote requests to the 1SB provider.
 * Implemented by adapter.onesb; only that package may hold 1SB types.
 */
public interface OneSbQuotePort {

    /**
     * Submits a quote request to 1SB and returns the external request ID (reqId).
     */
    String submitQuote(String jobId, CreateQuoteCommand command);

    /**
     * Polls for quote results by external request ID.
     *
     * @return non-empty list when poll is complete; empty when still in-progress
     */
    List<QuoteOffer> pollQuoteResult(String jobId, String externalReqId, String lob);

    boolean isPollComplete(String jobId, String externalReqId, String lob);
}
