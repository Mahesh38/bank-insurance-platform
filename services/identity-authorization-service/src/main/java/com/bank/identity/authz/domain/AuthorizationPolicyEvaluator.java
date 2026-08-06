package com.bank.identity.authz.domain;

import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Entitlement;
import com.bank.identity.authz.domain.AuthorizationModels.Facts;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuthorizationPolicyEvaluator {

    public Decision evaluate(Facts facts, DecisionRequest request, Instant now) {
        long version = facts.subject().policyVersion();
        if (facts.subject().status() != UserStatus.ACTIVE
            || now.isBefore(facts.subject().validFrom())
            || (facts.subject().validUntil() != null && !now.isBefore(facts.subject().validUntil()))) {
            return Decision.deny("ACCOUNT_NOT_ACTIVE", "lifecycle", version);
        }

        Resource resource = request.resource();
        if (facts.subject().userType() == UserType.INSURER_REPRESENTATIVE
            && hasText(resource.insurerCode())
            && !resource.insurerCode().equals(facts.subject().insurerCode())) {
            return Decision.deny("CROSS_INSURER_ACCESS", "tenant-boundary", version);
        }

        Entitlement explicitDeny = facts.entitlements().stream()
            .filter(it -> it.effect() == EntitlementEffect.DENY)
            .filter(it -> permissionMatches(it.permission(), request.action()))
            .filter(it -> scopeMatches(it, resource))
            .findFirst()
            .orElse(null);
        if (explicitDeny != null) {
            return Decision.deny("EXPLICIT_DENY", policyName(explicitDeny), version);
        }

        boolean accessAll = facts.entitlements().stream().anyMatch(it ->
            it.effect() == EntitlementEffect.GRANT
                && "*".equals(it.permission())
                && it.scopeType() == ScopeType.GLOBAL);

        if (hasText(resource.branchCode()) && !accessAll && !facts.branchCodes().contains(resource.branchCode())) {
            return Decision.deny("BRANCH_OUT_OF_SCOPE", "branch-boundary", version);
        }

        if (facts.subject().userType() == UserType.INSURER_REPRESENTATIVE
            && isPartnerResourceAction(request.action())
            && !resource.partnerVisible()) {
            return Decision.deny("RESOURCE_NOT_SHARED_WITH_PARTNER", "partner-sharing", version);
        }

        if (resource.regulatedAction()
            && facts.subject().userType() == UserType.BANK_EMPLOYEE
            && !facts.hasValidMandatoryCertification()) {
            return Decision.deny("MANDATORY_CERTIFICATION_INVALID", "certification-gate", version);
        }

        Entitlement directGrant = facts.entitlements().stream()
            .filter(it -> it.effect() == EntitlementEffect.GRANT)
            .filter(it -> permissionMatches(it.permission(), request.action()))
            .filter(it -> scopeMatches(it, resource))
            .findFirst()
            .orElse(null);
        if (directGrant != null) {
            return Decision.allow("DIRECT_SCOPED_GRANT", policyName(directGrant), version);
        }

        if (facts.rolePermissions().contains(request.action()) || facts.rolePermissions().contains("*")) {
            return Decision.allow("ROLE_GRANT", "role-permission", version);
        }

        return Decision.deny("DEFAULT_DENY", "default-deny", version);
    }

    private static boolean permissionMatches(String entitlementPermission, String requestedPermission) {
        return "*".equals(entitlementPermission) || entitlementPermission.equals(requestedPermission);
    }

    private static boolean scopeMatches(Entitlement entitlement, Resource resource) {
        return switch (entitlement.scopeType()) {
            case GLOBAL -> true;
            case BRANCH -> hasText(resource.branchCode()) && resource.branchCode().equals(entitlement.scopeValue());
            case INSURER -> hasText(resource.insurerCode()) && resource.insurerCode().equals(entitlement.scopeValue());
            case RESOURCE -> hasText(resource.type()) && hasText(resource.id())
                && (resource.type() + ":" + resource.id()).equals(entitlement.scopeValue());
        };
    }

    private static boolean isPartnerResourceAction(String action) {
        return action.startsWith("lead.") || action.startsWith("proposal.");
    }

    private static String policyName(Entitlement entitlement) {
        return "entitlement:" + entitlement.scopeType() + ":" + entitlement.scopeValue();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
