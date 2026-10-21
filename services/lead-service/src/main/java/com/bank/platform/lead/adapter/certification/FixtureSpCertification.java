package com.bank.platform.lead.adapter.certification;

import com.bank.platform.lead.domain.SpCertificationPort;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fail-closed fixture until the IRDAI / bank SP register is wired ({@code SUG-20261007-ird}). Only
 * listed empIds are treated as certified. This is not IRDAI control evidence.
 */
@Component
@ConditionalOnProperty(
    name = "lead.sp-certification-mode",
    havingValue = "fixture",
    matchIfMissing = true)
public class FixtureSpCertification implements SpCertificationPort {

  static final Set<String> CERTIFIED = Set.of("SP-1001", "SP-1002", "SP-2001");

  @Override
  public boolean holdsValidCertification(String empId) {
    return empId != null && CERTIFIED.contains(empId);
  }
}
