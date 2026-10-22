package com.bank.platform.lead.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The customer and seller facts behind {@code LEAD.EXCEPTION} (Exception Handling BRD §6.3, §18
 * EH-INT-001…005: CBS, Equifax, DWH, insurer policy data, the restriction lists).
 *
 * <p>A fact is one of three things, and the difference matters (BRD §24.3 column I, E9): a value; a
 * known absence ({@code null} field, fact <em>not</em> in {@link Facts#unknown()} — e.g. no lapsed
 * policy); or <em>unknown</em> (listed in {@code unknown()} — the source did not answer). An unknown
 * fact blocks the lead; it is never read as "no breach".
 */
public interface LeadExceptionFactsPort {

  Facts factsFor(ExceptionRulesPort.Subject subject);

  /**
   * Input names of {@code LEAD.EXCEPTION}. {@code premiumCaptureOnly}: not available at Start
   * Onboarding by design (BRD §6.2), so unknown there is not a gap.
   */
  enum Fact {
    PROPOSER_AGE("proposerAge", false),
    HAS_BSBD_ACCOUNT("hasBsbdAccount", false),
    EQUIFAX_SCORE("equifaxScore", false),
    LAST_LAPSED_POLICY_CATEGORY("lastLapsedPolicyCategory", false),
    MONTHS_SINCE_LAST_LAPSE("monthsSinceLastLapse", false),
    MONTHS_SINCE_LAST_TERMINATION("monthsSinceLastTermination", false),
    ACTIVE_POLICY_COUNT("activePolicyCount", false),
    CASA_OPENED_ON("casaOpenedOn", false),
    LAST_ASSET_ACCOUNT_OPENED_ON("lastAssetAccountOpenedOn", false),
    HAS_ACTIVE_RELATIONSHIP("hasActiveRelationship", false),
    IS_AU_EMPLOYEE("isAuEmployee", false),
    NEXT_RENEWAL_DUE_ON("nextRenewalDueOn", false),
    OLDEST_UNPAID_RENEWAL_DUE_ON("oldestUnpaidRenewalDueOn", true),
    ANNUAL_PREMIUM("annualPremium", true),
    SELLER_RESTRICTED("sellerRestricted", false),
    CUSTOMER_RESTRICTED("customerRestricted", false);

    private final String inputName;
    private final boolean premiumCaptureOnly;

    Fact(String inputName, boolean premiumCaptureOnly) {
      this.inputName = inputName;
      this.premiumCaptureOnly = premiumCaptureOnly;
    }

    public String inputName() {
      return inputName;
    }

    public boolean premiumCaptureOnly() {
      return premiumCaptureOnly;
    }
  }

  record Facts(
      Integer proposerAge,
      Boolean hasBsbdAccount,
      Integer equifaxScore,
      String lastLapsedPolicyCategory,
      Integer monthsSinceLastLapse,
      Integer monthsSinceLastTermination,
      Integer activePolicyCount,
      LocalDate casaOpenedOn,
      LocalDate lastAssetAccountOpenedOn,
      Boolean hasActiveRelationship,
      Boolean isAuEmployee,
      LocalDate nextRenewalDueOn,
      LocalDate oldestUnpaidRenewalDueOn,
      BigDecimal annualPremium,
      Boolean sellerRestricted,
      Boolean customerRestricted,
      Set<Fact> unknown) {

    public Facts {
      unknown = unknown == null || unknown.isEmpty() ? Set.of() : Set.copyOf(unknown);
    }

    /** Nothing is known — every lead blocks (BRD §24.3 column I). */
    public static Facts allUnknown() {
      return new Facts(
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          null, EnumSet.allOf(Fact.class));
    }

    public Object value(Fact fact) {
      Objects.requireNonNull(fact);
      return switch (fact) {
        case PROPOSER_AGE -> proposerAge;
        case HAS_BSBD_ACCOUNT -> hasBsbdAccount;
        case EQUIFAX_SCORE -> equifaxScore;
        case LAST_LAPSED_POLICY_CATEGORY -> lastLapsedPolicyCategory;
        case MONTHS_SINCE_LAST_LAPSE -> monthsSinceLastLapse;
        case MONTHS_SINCE_LAST_TERMINATION -> monthsSinceLastTermination;
        case ACTIVE_POLICY_COUNT -> activePolicyCount;
        case CASA_OPENED_ON -> casaOpenedOn;
        case LAST_ASSET_ACCOUNT_OPENED_ON -> lastAssetAccountOpenedOn;
        case HAS_ACTIVE_RELATIONSHIP -> hasActiveRelationship;
        case IS_AU_EMPLOYEE -> isAuEmployee;
        case NEXT_RENEWAL_DUE_ON -> nextRenewalDueOn;
        case OLDEST_UNPAID_RENEWAL_DUE_ON -> oldestUnpaidRenewalDueOn;
        case ANNUAL_PREMIUM -> annualPremium;
        case SELLER_RESTRICTED -> sellerRestricted;
        case CUSTOMER_RESTRICTED -> customerRestricted;
      };
    }
  }
}
