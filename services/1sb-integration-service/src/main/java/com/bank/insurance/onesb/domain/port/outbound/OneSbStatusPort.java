package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.OneSbApplicationStatusResult;

import java.util.Optional;

/**
 * Outbound port for application-status lookup via 1SB.
 */
public interface OneSbStatusPort {

    /**
     * @return empty when 1SB has no status for {@code applicationNumber} (1SB returns 200 with an
     *         empty result rather than 404 — see field-guides/application-status.md)
     */
    Optional<OneSbApplicationStatusResult> getStatus(String applicationNumber, String insurerCode,
                                                      String productCode);
}
