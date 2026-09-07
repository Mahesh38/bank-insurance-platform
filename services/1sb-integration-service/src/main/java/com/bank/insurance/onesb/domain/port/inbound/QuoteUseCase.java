package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.common.domain.QuoteJob;

/**
 * Inbound use-case interface for quote operations.
 * Optional; allows the application layer to be tested via interface only.
 */
public interface QuoteUseCase {

    /**
     * Creates a quote job and initiates async polling.
     *
     * @return the bank-assigned jobId
     */
    String createQuote(CreateQuoteCommand command);

    /**
     * Returns the quote job for bank polling (any status including TIMEOUT).
     * Unknown jobId → {@code RESOURCE_NOT_FOUND} (404).
     */
    QuoteJob getQuoteResult(String jobId);
}
