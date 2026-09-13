package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.UlipFundResult;

/**
 * Outbound port for documented ULIP fund list / performance POSTs.
 */
public interface OneSbUlipFundPort {

    UlipFundResult listFunds(Object payload);

    UlipFundResult fundPerformance(Object payload);
}
