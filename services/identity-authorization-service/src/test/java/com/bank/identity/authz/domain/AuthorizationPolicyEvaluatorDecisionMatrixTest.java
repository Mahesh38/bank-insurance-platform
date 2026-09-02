package com.bank.identity.authz.domain;

import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Entitlement;
import com.bank.identity.authz.domain.AuthorizationModels.Facts;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.AuthorizationModels.Subject;
import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decision-matrix coverage for the PDP.
 *
 * <p>{@link AuthorizationPolicyEvaluatorTest} covers the six headline scenarios. This suite
 * covers the <b>branches</b> — the ordering between gates, the boundary conditions on account
 * validity, every {@link ScopeType} arm, and above all the terminal {@code DEFAULT_DENY}.
 *
 * <p>Why this class earns its place rather than being coverage filler: this evaluator is the
 * policy decision point for the whole platform, and WS-2 gate criterion A.3 requires that
 * "the authorization service is the PDP and default-deny is verified". Default-deny is not
 * verified by a class existing; it is verified by showing that every path which does not
 * explicitly match a grant returns a denial. That is what the {@code DefaultDeny} nested class
 * below does.
 *
 * <p>Ordering matters as much as outcome here. An explicit deny must beat a role grant; a
 * tenant-boundary violation must be caught before any grant is consulted; and a wildcard
 * global grant must not silently bypass the certification gate. Each of those is asserted as
 * a precedence, not just as a result.
 */
class AuthorizationPolicyEvaluatorDecisionMatrixTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("b06fd0eb-30c5-4782-9aad-9b2da6096680");
    private static final long POLICY_VERSION = 7L;

    private final AuthorizationPolicyEvaluator evaluator = new AuthorizationPolicyEvaluator();

    // -----------------------------------------------------------------------
    // Gate 1 — account lifecycle
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Account lifecycle gate runs before anything else")
    class Lifecycle {

        @ParameterizedTest(name = "status {0} is denied even with a matching role permission")
        @EnumSource(value = UserStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
        void nonActiveStatusIsAlwaysDenied(UserStatus status) {
            Facts facts = new Facts(
                subject(UserType.BANK_EMPLOYEE, status, null, NOW.minusSeconds(60), null),
                Set.of("lead.read"),
                List.of(grant("*", ScopeType.GLOBAL, null)),
                Set.of("BELAPUR"),
                true);

            Decision decision = evaluator.evaluate(facts, request("lead.read", "BELAPUR", null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
            assertThat(decision.matchedPolicy()).isEqualTo("lifecycle");
            assertThat(decision.policyVersion()).isEqualTo(POLICY_VERSION);
        }

        @Test
        void accountNotYetValidIsDenied() {
            Facts facts = activeFacts(NOW.plusSeconds(3600), null, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
        }

        @Test
        void accountPastValidUntilIsDenied() {
            Facts facts = activeFacts(NOW.minusSeconds(3600), NOW.minusSeconds(1), Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
        }

        @Test
        @DisplayName("validUntil is exclusive: an account expiring exactly now is already denied")
        void validUntilBoundaryIsExclusive() {
            Facts facts = activeFacts(NOW.minusSeconds(3600), NOW, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
        }

        @Test
        void accountInsideItsValidityWindowProceeds() {
            Facts facts = activeFacts(NOW.minusSeconds(60), NOW.plusSeconds(60), Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
        }
    }

    // -----------------------------------------------------------------------
    // Gate 2 — insurer tenant boundary
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Insurer tenant boundary")
    class TenantBoundary {

        @Test
        void partnerMayActWithinItsOwnInsurer() {
            Facts facts = partnerFacts("HDFC", Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, "HDFC", true, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
        }

        @Test
        @DisplayName("a resource with no insurer code is not subject to the tenant gate")
        void blankResourceInsurerSkipsTenantGate() {
            Facts facts = partnerFacts("HDFC", Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, "  ", true, false), NOW);

            assertThat(decision.allowed()).isTrue();
        }

        @Test
        @DisplayName("bank employees are not constrained by the insurer tenant gate")
        void bankEmployeeCrossesInsurerFreely() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, "ICICI", true, false), NOW);

            assertThat(decision.allowed()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Gate 3 — branch scope, and the global-wildcard bypass
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Branch scope")
    class BranchScope {

        @Test
        void branchOutsideAssignedSetIsDenied() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
            assertThat(decision.matchedPolicy()).isEqualTo("branch-boundary");
        }

        @Test
        @DisplayName("a GLOBAL wildcard grant bypasses the branch gate — this is the break-glass path")
        void globalWildcardGrantBypassesBranchGate() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(),
                List.of(grant("*", ScopeType.GLOBAL, null)));

            Decision decision = evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("DIRECT_SCOPED_GRANT");
        }

        @Test
        @DisplayName("a non-global wildcard grant does NOT bypass the branch gate")
        void branchScopedWildcardDoesNotBypassBranchGate() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(),
                List.of(grant("*", ScopeType.BRANCH, "KAMOTHE")));

            Decision decision = evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
        }

        @Test
        void resourceWithNoBranchSkipsTheBranchGate() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Gate 4/5 — partner sharing and the certification gate
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Partner sharing and certification gates")
    class PartnerAndCertification {

        @Test
        void partnerIsDeniedAnUnsharedProposal() {
            Facts facts = partnerFacts("HDFC", Set.of("proposal.submit"), List.of());

            Decision decision = evaluator.evaluate(facts, request("proposal.submit", null, "HDFC", false, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("RESOURCE_NOT_SHARED_WITH_PARTNER");
        }

        @Test
        @DisplayName("the partner-sharing gate applies only to lead.* and proposal.* actions")
        void nonPartnerScopedActionIgnoresSharingFlag() {
            Facts facts = partnerFacts("HDFC", Set.of("report.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("report.read", null, "HDFC", false, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
        }

        @Test
        void uncertifiedBankEmployeeIsDeniedARegulatedAction() {
            Facts facts = new Facts(
                subject(UserType.BANK_EMPLOYEE, UserStatus.ACTIVE, null, NOW.minusSeconds(60), null),
                Set.of("proposal.submit"), List.of(), Set.of(), false);

            Decision decision = evaluator.evaluate(facts, request("proposal.submit", null, null, true, true), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("MANDATORY_CERTIFICATION_INVALID");
            assertThat(decision.matchedPolicy()).isEqualTo("certification-gate");
        }

        @Test
        @DisplayName("the certification gate is a bank-employee control; partners are governed by their own insurer")
        void uncertifiedPartnerIsNotBlockedByTheBankCertificationGate() {
            Facts facts = new Facts(
                subject(UserType.INSURER_REPRESENTATIVE, UserStatus.ACTIVE, "HDFC", NOW.minusSeconds(60), null),
                Set.of("proposal.submit"), List.of(), Set.of(), false);

            Decision decision = evaluator.evaluate(facts, request("proposal.submit", null, "HDFC", true, true), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
        }
    }

    // -----------------------------------------------------------------------
    // Gate 6 — scoped grants, every ScopeType arm
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Scoped entitlement matching")
    class ScopedGrants {

        @Test
        void globalScopeGrantMatches() {
            assertGranted(grant("lead.read", ScopeType.GLOBAL, null),
                request("lead.read", null, null, true, false));
        }

        @Test
        void branchScopeGrantMatchesTheSameBranch() {
            Facts facts = new Facts(
                subject(UserType.BANK_EMPLOYEE, UserStatus.ACTIVE, null, NOW.minusSeconds(60), null),
                Set.of(), List.of(grant("lead.read", ScopeType.BRANCH, "BELAPUR")), Set.of("BELAPUR"), true);

            Decision decision = evaluator.evaluate(facts, request("lead.read", "BELAPUR", null, true, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("DIRECT_SCOPED_GRANT");
            assertThat(decision.matchedPolicy()).isEqualTo("entitlement:BRANCH:BELAPUR");
        }

        @Test
        void insurerScopeGrantMatchesTheSameInsurer() {
            assertGranted(grant("lead.read", ScopeType.INSURER, "HDFC"),
                request("lead.read", null, "HDFC", true, false));
        }

        @Test
        void resourceScopeGrantMatchesTypeAndIdPair() {
            assertGranted(grant("lead.read", ScopeType.RESOURCE, "LEAD:lead-1"),
                request("lead.read", null, null, true, false));
        }

        @Test
        void resourceScopeGrantForADifferentResourceDoesNotMatch() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(),
                List.of(grant("lead.read", ScopeType.RESOURCE, "LEAD:lead-999")));

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("DEFAULT_DENY");
        }

        @Test
        @DisplayName("a wildcard permission on an entitlement matches any action within its scope")
        void wildcardPermissionEntitlementMatchesAnyAction() {
            assertGranted(grant("*", ScopeType.INSURER, "HDFC"),
                request("anything.at.all", null, "HDFC", true, false));
        }

        private void assertGranted(Entitlement entitlement, DecisionRequest req) {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(), List.of(entitlement));

            Decision decision = evaluator.evaluate(facts, req, NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("DIRECT_SCOPED_GRANT");
        }
    }

    // -----------------------------------------------------------------------
    // Gate 7 — the terminal rule. WS-2 criterion A.3.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Default deny is the terminal rule")
    class DefaultDeny {

        @Test
        @DisplayName("no role permission, no entitlement, nothing matched -> DEFAULT_DENY")
        void nothingMatchedIsDenied() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("DEFAULT_DENY");
            assertThat(decision.matchedPolicy()).isEqualTo("default-deny");
        }

        @Test
        @DisplayName("a role permission for a DIFFERENT action does not leak into this one")
        void unrelatedRolePermissionDoesNotGrant() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.delete", null, null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("DEFAULT_DENY");
        }

        @Test
        @DisplayName("a grant whose scope does not match the resource falls through to DEFAULT_DENY")
        void grantWithNonMatchingScopeFallsThrough() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of(),
                List.of(grant("lead.read", ScopeType.INSURER, "ICICI")));

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, "HDFC", true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("DEFAULT_DENY");
        }

        @Test
        @DisplayName("wildcard role permission is a grant of last resort")
        void wildcardRolePermissionGrants() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("*"), List.of());

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);

            assertThat(decision.allowed()).isTrue();
            assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
            assertThat(decision.matchedPolicy()).isEqualTo("role-permission");
        }
    }

    // -----------------------------------------------------------------------
    // Precedence — the orderings that make the model safe
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Gate precedence")
    class Precedence {

        @Test
        @DisplayName("an explicit deny beats a global wildcard grant")
        void explicitDenyBeatsGlobalWildcardGrant() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"),
                List.of(grant("*", ScopeType.GLOBAL, null),
                        new Entitlement(EntitlementEffect.DENY, "lead.read", ScopeType.GLOBAL, null)));

            Decision decision = evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("EXPLICIT_DENY");
        }

        @Test
        @DisplayName("the tenant boundary is evaluated before any grant is consulted")
        void tenantBoundaryBeatsGlobalWildcardGrant() {
            Facts facts = new Facts(
                subject(UserType.INSURER_REPRESENTATIVE, UserStatus.ACTIVE, "HDFC", NOW.minusSeconds(60), null),
                Set.of("*"), List.of(grant("*", ScopeType.GLOBAL, null)), Set.of(), true);

            Decision decision = evaluator.evaluate(facts, request("lead.read", null, "ICICI", true, false), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("CROSS_INSURER_ACCESS");
            assertThat(decision.matchedPolicy()).isEqualTo("tenant-boundary");
        }

        @Test
        @DisplayName("the certification gate is evaluated before a scoped grant can allow a regulated action")
        void certificationGateBeatsScopedGrant() {
            Facts facts = new Facts(
                subject(UserType.BANK_EMPLOYEE, UserStatus.ACTIVE, null, NOW.minusSeconds(60), null),
                Set.of(), List.of(grant("proposal.submit", ScopeType.GLOBAL, null)), Set.of(), false);

            Decision decision = evaluator.evaluate(facts, request("proposal.submit", null, null, true, true), NOW);

            assertThat(decision.allowed()).isFalse();
            assertThat(decision.reasonCode()).isEqualTo("MANDATORY_CERTIFICATION_INVALID");
        }

        @Test
        @DisplayName("every decision carries the policy version it was evaluated against")
        void everyDecisionCarriesThePolicyVersion() {
            Facts facts = activeFacts(NOW.minusSeconds(60), null, Set.of("lead.read"), List.of());

            Decision allowed = evaluator.evaluate(facts, request("lead.read", null, null, true, false), NOW);
            Decision denied = evaluator.evaluate(facts, request("lead.delete", null, null, true, false), NOW);

            assertThat(allowed.policyVersion()).isEqualTo(POLICY_VERSION);
            assertThat(denied.policyVersion()).isEqualTo(POLICY_VERSION);
        }
    }

    // -----------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------

    private static Subject subject(UserType type, UserStatus status, String insurer,
                                   Instant validFrom, Instant validUntil) {
        return new Subject(USER_ID, type, status, insurer, POLICY_VERSION, validFrom, validUntil);
    }

    private static Facts activeFacts(Instant validFrom, Instant validUntil,
                                     Set<String> permissions, List<Entitlement> entitlements) {
        return new Facts(subject(UserType.BANK_EMPLOYEE, UserStatus.ACTIVE, null, validFrom, validUntil),
            permissions, entitlements, Set.of("BELAPUR"), true);
    }

    private static Facts partnerFacts(String insurer, Set<String> permissions, List<Entitlement> entitlements) {
        return new Facts(subject(UserType.INSURER_REPRESENTATIVE, UserStatus.ACTIVE, insurer,
            NOW.minusSeconds(60), null), permissions, entitlements, Set.of(), true);
    }

    private static Entitlement grant(String permission, ScopeType scopeType, String scopeValue) {
        return new Entitlement(EntitlementEffect.GRANT, permission, scopeType, scopeValue);
    }

    private static DecisionRequest request(String action, String branch, String insurer,
                                           boolean partnerVisible, boolean regulated) {
        return new DecisionRequest(USER_ID, action,
            new Resource("LEAD", "lead-1", branch, insurer, "rm-a", Set.of("rm-a"), partnerVisible, regulated));
    }
}
