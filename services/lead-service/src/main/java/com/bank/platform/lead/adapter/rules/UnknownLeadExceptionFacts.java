package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.ExceptionRulesPort.Subject;
import com.bank.platform.lead.domain.LeadExceptionFactsPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stand-in until the real fact sources exist: reports <em>every</em> fact as unknown, so every lead
 * evaluates to {@code BLOCK / RULE_INPUT_MISSING} (BRD §24.3 column I, E9). It can never pass a
 * customer, which is why it is safe as the default.
 *
 * <p>TODO adapters, one per Exception Handling BRD §18 interface (contracts, timeouts and error
 * mappings are "to be confirmed" in the BRD; owners in the lead LLD §9):
 *
 * <ul>
 *   <li>EH-INT-001 Customer 360 — {@code casaOpenedOn}, {@code hasActiveRelationship}, {@code
 *       hasBsbdAccount}, {@code proposerAge} (date of birth; via customer-service / CBS);
 *   <li>EH-INT-002 Loan / Credit Data Service — {@code lastAssetAccountOpenedOn};
 *   <li>EH-INT-003 CustomerCIFStatusCheck — {@code isAuEmployee} (StaffFlag);
 *   <li>EH-INT-004 Policy Master / DataMigration_Life — {@code activePolicyCount}, lapsed and
 *       terminated policies;
 *   <li>EH-INT-005 Renewal Module — {@code nextRenewalDueOn}, {@code oldestUnpaidRenewalDueOn};
 *   <li>no BRD interface yet — {@code equifaxScore} (Equifax), {@code sellerRestricted} and {@code
 *       customerRestricted} (restriction lists; BRD §5.2 admin uploads).
 * </ul>
 *
 * <p>Technical failure of any source must not permit progression (BRD §18): a real adapter reports
 * a fact it could not obtain as unknown.
 */
@Component
@ConditionalOnProperty(
    name = "lead.exception-facts",
    havingValue = "unknown",
    matchIfMissing = true)
public class UnknownLeadExceptionFacts implements LeadExceptionFactsPort {

  @Override
  public Facts factsFor(Subject subject) {
    return Facts.allUnknown();
  }
}
