package com.bank.identity.authz.domain;

import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Entitlement;
import com.bank.identity.authz.domain.AuthorizationModels.Facts;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.AuthorizationModels.Subject;
import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationPolicyEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-06T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("b06fd0eb-30c5-4782-9aad-9b2da6096680");
    private final AuthorizationPolicyEvaluator evaluator = new AuthorizationPolicyEvaluator();

    @Test
    void explicitScopedDenyOverridesRoleGrant() {
        var facts = facts(UserType.BANK_EMPLOYEE, null, Set.of("lead.read"),
            List.of(new Entitlement(EntitlementEffect.DENY, "lead.read", ScopeType.BRANCH, "BELAPUR")),
            Set.of("BELAPUR"), true);

        var decision = evaluator.evaluate(facts, request("lead.read", "BELAPUR", null, true, false), NOW);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reasonCode()).isEqualTo("EXPLICIT_DENY");
    }

    @Test
    void deniesRmOutsideAssignedBranch() {
        var facts = facts(UserType.BANK_EMPLOYEE, null, Set.of("lead.read"), List.of(),
            Set.of("BELAPUR", "KHARGHAR"), true);

        var decision = evaluator.evaluate(facts, request("lead.read", "KAMOTHE", null, true, false), NOW);

        assertThat(decision.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
    }

    @Test
    void deniesPartnerAcrossInsurerEvenWithPermission() {
        var facts = facts(UserType.INSURER_REPRESENTATIVE, "ICICI_PRU", Set.of("lead.read"), List.of(),
            Set.of("BELAPUR"), true);

        var decision = evaluator.evaluate(facts, request("lead.read", "BELAPUR", "HDFC_LIFE", true, false), NOW);

        assertThat(decision.reasonCode()).isEqualTo("CROSS_INSURER_ACCESS");
    }

    @Test
    void deniesUnsharedLeadToPartner() {
        var facts = facts(UserType.INSURER_REPRESENTATIVE, "ICICI_PRU", Set.of("lead.read"), List.of(),
            Set.of("BELAPUR"), true);

        var decision = evaluator.evaluate(facts, request("lead.read", "BELAPUR", "ICICI_PRU", false, false), NOW);

        assertThat(decision.reasonCode()).isEqualTo("RESOURCE_NOT_SHARED_WITH_PARTNER");
    }

    @Test
    void deniesRegulatedBankActionWithoutValidCertificate() {
        var facts = facts(UserType.BANK_EMPLOYEE, null, Set.of("proposal.submit"), List.of(),
            Set.of("BELAPUR"), false);

        var decision = evaluator.evaluate(facts, request("proposal.submit", "BELAPUR", "ICICI_PRU", true, true), NOW);

        assertThat(decision.reasonCode()).isEqualTo("MANDATORY_CERTIFICATION_INVALID");
    }

    @Test
    void allowsRolePermissionInsideBranchAndTenantBoundaries() {
        var facts = facts(UserType.INSURER_REPRESENTATIVE, "ICICI_PRU", Set.of("proposal.view"), List.of(),
            Set.of("BELAPUR"), false);

        var decision = evaluator.evaluate(facts, request("proposal.view", "BELAPUR", "ICICI_PRU", true, false), NOW);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
    }

    private static Facts facts(
        UserType type,
        String insurer,
        Set<String> permissions,
        List<Entitlement> entitlements,
        Set<String> branches,
        boolean certified
    ) {
        return new Facts(new Subject(USER_ID, type, UserStatus.ACTIVE, insurer, 3,
            NOW.minusSeconds(60), null), permissions, entitlements, branches, certified);
    }

    private static DecisionRequest request(
        String action,
        String branch,
        String insurer,
        boolean partnerVisible,
        boolean regulated
    ) {
        return new DecisionRequest(USER_ID, action,
            new Resource("LEAD", "lead-1", branch, insurer, "rm-a", Set.of("rm-a"),
                partnerVisible, regulated));
    }
}
