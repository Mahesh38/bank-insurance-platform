package com.bank.identity.authz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Entitlement;
import com.bank.identity.authz.domain.AuthorizationModels.Facts;
import com.bank.identity.authz.domain.AuthorizationModels.PartnerActionRestriction;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.AuthorizationModels.Subject;
import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationPolicyEvaluatorTest {

  private static final Instant NOW = Instant.parse("2026-08-06T10:00:00Z");
  // Mirrors the strict seed in application.yml (identity.authorization.partner-representative).
  static final PartnerActionRestriction STRICT_PARTNER_RESTRICTION =
      new PartnerActionRestriction(
          Set.of("partner_user.create", "partner_user.bulk_import"), List.of(".approve"));
  private static final UUID USER_ID = UUID.fromString("b06fd0eb-30c5-4782-9aad-9b2da6096680");
  private final AuthorizationPolicyEvaluator evaluator =
      new AuthorizationPolicyEvaluator(STRICT_PARTNER_RESTRICTION);

  @Test
  void explicitScopedDenyOverridesRoleGrant() {
    var facts =
        facts(
            UserType.BANK_EMPLOYEE,
            null,
            Set.of("lead.read"),
            List.of(
                new Entitlement(EntitlementEffect.DENY, "lead.read", ScopeType.BRANCH, "BELAPUR")),
            Set.of("BELAPUR"),
            true);

    var decision =
        evaluator.evaluate(facts, request("lead.read", "BELAPUR", null, true, false), NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("EXPLICIT_DENY");
  }

  @Test
  void deniesRmOutsideAssignedBranch() {
    var facts =
        facts(
            UserType.BANK_EMPLOYEE,
            null,
            Set.of("lead.read"),
            List.of(),
            Set.of("BELAPUR", "KHARGHAR"),
            true);

    var decision =
        evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

    assertThat(decision.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
  }

  @Test
  void deniesPartnerAcrossInsurerEvenWithPermission() {
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("lead.read"),
            List.of(),
            Set.of("BELAPUR"),
            true);

    var decision =
        evaluator.evaluate(facts, request("lead.read", "BELAPUR", "HDFC_LIFE", true, false), NOW);

    assertThat(decision.reasonCode()).isEqualTo("CROSS_INSURER_ACCESS");
  }

  @Test
  void deniesUnsharedLeadToPartner() {
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("lead.read"),
            List.of(),
            Set.of("BELAPUR"),
            true);

    var decision =
        evaluator.evaluate(facts, request("lead.read", "BELAPUR", "ICICI_PRU", false, false), NOW);

    assertThat(decision.reasonCode()).isEqualTo("RESOURCE_NOT_SHARED_WITH_PARTNER");
  }

  @Test
  void deniesRegulatedBankActionWithoutValidCertificate() {
    var facts =
        facts(
            UserType.BANK_EMPLOYEE,
            null,
            Set.of("proposal.submit"),
            List.of(),
            Set.of("BELAPUR"),
            false);

    var decision =
        evaluator.evaluate(
            facts, request("proposal.submit", "BELAPUR", "ICICI_PRU", true, true), NOW);

    assertThat(decision.reasonCode()).isEqualTo("MANDATORY_CERTIFICATION_INVALID");
  }

  @Test
  void allowsRolePermissionInsideBranchAndTenantBoundaries() {
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("proposal.view"),
            List.of(),
            Set.of("BELAPUR"),
            false);

    var decision =
        evaluator.evaluate(
            facts, request("proposal.view", "BELAPUR", "ICICI_PRU", true, false), NOW);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
  }

  @Test
  void partnerRepresentativeCanNeverApproveALeadExceptionEvenWithAnExplicitGrant() {
    // E6: role permission, scoped GRANT and a global `*` grant together still do not suffice.
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("lead.exception.approve", "*"),
            List.of(
                new Entitlement(
                    EntitlementEffect.GRANT, "lead.exception.approve", ScopeType.BRANCH, "BELAPUR"),
                new Entitlement(EntitlementEffect.GRANT, "*", ScopeType.GLOBAL, null)),
            Set.of("BELAPUR"),
            true);

    var decision =
        evaluator.evaluate(
            facts, request("lead.exception.approve", "BELAPUR", "ICICI_PRU", true, false), NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("PARTNER_ACTION_PROHIBITED");
    assertThat(decision.matchedPolicy()).isEqualTo("partner-prohibited-action");
  }

  @Test
  void partnerRepresentativeCanNeverCreatePartnerUsers() {
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("partner_user.create"),
            List.of(),
            Set.of(),
            true);

    var decision =
        evaluator.evaluate(
            facts, request("partner_user.create", null, "ICICI_PRU", true, false), NOW);

    assertThat(decision.reasonCode()).isEqualTo("PARTNER_ACTION_PROHIBITED");
  }

  @Test
  void bankEmployeeInTheBranchMayApproveALeadException() {
    var facts =
        facts(
            UserType.BANK_EMPLOYEE,
            null,
            Set.of("lead.exception.approve"),
            List.of(),
            Set.of("BELAPUR"),
            true);

    var allowed =
        evaluator.evaluate(
            facts, request("lead.exception.approve", "BELAPUR", null, false, false), NOW);
    var otherBranch =
        evaluator.evaluate(
            facts, request("lead.exception.approve", "KAMOTHE", null, false, false), NOW);

    assertThat(allowed.allowed()).isTrue();
    assertThat(otherBranch.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
  }

  @Test
  void prohibitedSuffixesAreConfigurationNotCode() {
    var relaxed =
        new AuthorizationPolicyEvaluator(
            new PartnerActionRestriction(Set.of(), List.of(".reject")));
    var facts =
        facts(
            UserType.INSURER_REPRESENTATIVE,
            "ICICI_PRU",
            Set.of("lead.exception.approve", "lead.exception.reject"),
            List.of(),
            Set.of("BELAPUR"),
            true);

    assertThat(
            relaxed
                .evaluate(
                    facts,
                    request("lead.exception.reject", "BELAPUR", "ICICI_PRU", true, false),
                    NOW)
                .reasonCode())
        .isEqualTo("PARTNER_ACTION_PROHIBITED");
    assertThat(
            relaxed
                .evaluate(
                    facts,
                    request("lead.exception.approve", "BELAPUR", "ICICI_PRU", true, false),
                    NOW)
                .allowed())
        .isTrue();
  }

  private static Facts facts(
      UserType type,
      String insurer,
      Set<String> permissions,
      List<Entitlement> entitlements,
      Set<String> branches,
      boolean certified) {
    return new Facts(
        new Subject(USER_ID, type, UserStatus.ACTIVE, insurer, 3, NOW.minusSeconds(60), null),
        permissions,
        entitlements,
        branches,
        certified);
  }

  private static DecisionRequest request(
      String action, String branch, String insurer, boolean partnerVisible, boolean regulated) {
    return new DecisionRequest(
        USER_ID,
        action,
        new Resource(
            "LEAD", "lead-1", branch, insurer, "rm-a", Set.of("rm-a"), partnerVisible, regulated));
  }
}
