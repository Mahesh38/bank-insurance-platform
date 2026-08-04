package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.Lob;

/**
 * Inbound use-case for application status lookup (FUNC-009).
 */
public interface StatusUseCase {

    /**
     * Looks up and normalises application status. Live 1SB call each time — not cached.
     *
     * @throws com.bank.common.error.ServiceException 404 {@code RESOURCE_NOT_FOUND} when 1SB
     *         has no status for the given {@code applicationNumber}
     */
    ApplicationStatus getStatus(String applicationNumber, Lob lob, String insurerCode,
                               String productCode, String actorId);
}
