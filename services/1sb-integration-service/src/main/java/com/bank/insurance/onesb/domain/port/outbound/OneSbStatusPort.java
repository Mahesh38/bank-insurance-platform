package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.ApplicationStatus;

/**
 * Outbound port for 1SB application status ({@code POST /LifeTerm/prostat/}).
 */
public interface OneSbStatusPort {

    ApplicationStatus fetchStatus(String applicationNumber, String lob,
                                  String insuranceCompanyCode, String policyNo);
}
