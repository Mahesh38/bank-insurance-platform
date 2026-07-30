package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.QuoteOffer;

import java.util.List;

/**
 * Outbound port for submitting quote requests to the 1SB provider.
 * Implemented by adapter.onesb; only that package may hold 1SB types.
 * <p>
 * Case 2: the application service resolves the LOB handler and builds the
 * submit payload/path; the adapter posts what it is given (no handler re-resolve on submit).
 */
public interface OneSbQuotePort {

    /**
     * Posts {@code payload} to the given relative 1SB {@code path} and returns the external request ID (reqId).
     */
    String submitQuote(String jobId, String path, Object payload);

    /**
     * Polls for quote results by external request ID.
     *
     * @return non-empty list when poll is complete; empty when still in-progress
     */
    List<QuoteOffer> pollQuoteResult(String jobId, String externalReqId, String lob);

    boolean isPollComplete(String jobId, String externalReqId, String lob);
}
