package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.UlipFundResult;

/**
 * Documented ULIP fund helpers on the Saving API (list + performance).
 */
public interface UlipFundUseCase {

    UlipFundResult listFunds(CreateQuoteCommand command);

    UlipFundResult fundPerformance(CreateQuoteCommand command);
}
