package com.bank.identity.authz.domain;

import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthorizationModels {
    private AuthorizationModels() {}

    public record Subject(
        UUID id,
        UserType userType,
        UserStatus status,
        String insurerCode,
        long policyVersion,
        Instant validFrom,
        Instant validUntil
    ) {}

    public record Entitlement(
        EntitlementEffect effect,
        String permission,
        ScopeType scopeType,
        String scopeValue
    ) {}

    public record Resource(
        String type,
        String id,
        String branchCode,
        String insurerCode,
        String ownerId,
        Set<String> assignedUserIds,
        boolean partnerVisible,
        boolean regulatedAction
    ) {
        public Resource {
            assignedUserIds = assignedUserIds == null ? Set.of() : Set.copyOf(assignedUserIds);
        }
    }

    public record DecisionRequest(UUID subjectId, String action, Resource resource) {}

    public record Facts(
        Subject subject,
        Set<String> rolePermissions,
        List<Entitlement> entitlements,
        Set<String> branchCodes,
        boolean hasValidMandatoryCertification
    ) {}

    public record Decision(
        boolean allowed,
        String reasonCode,
        String matchedPolicy,
        long policyVersion
    ) {
        static Decision allow(String reason, String policy, long version) {
            return new Decision(true, reason, policy, version);
        }

        static Decision deny(String reason, String policy, long version) {
            return new Decision(false, reason, policy, version);
        }
    }
}
