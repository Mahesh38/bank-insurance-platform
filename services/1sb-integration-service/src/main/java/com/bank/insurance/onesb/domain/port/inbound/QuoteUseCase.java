package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.QuoteJob;

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

    QuoteJob getQuoteResult(String jobId);
}
