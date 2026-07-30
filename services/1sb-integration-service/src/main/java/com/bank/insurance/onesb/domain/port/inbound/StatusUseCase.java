package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.GetApplicationStatusCommand;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;

/**
 * Inbound port for application status lookup (FUNC-009).
 */
public interface StatusUseCase {

    ApplicationStatus getStatus(GetApplicationStatusCommand command);
}
