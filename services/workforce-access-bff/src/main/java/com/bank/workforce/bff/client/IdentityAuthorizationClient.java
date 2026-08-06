package com.bank.workforce.bff.client;

import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.session.SessionModels.IdentitySource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class IdentityAuthorizationClient {

    private final RestClient client;

    public IdentityAuthorizationClient(RestClient restClient, DownstreamProperties properties) {
        this.client = restClient.mutate().baseUrl(properties.authorizationServiceBaseUrl().toString()).build();
    }

    public ResolvedIdentity resolve(IdentitySource source, IdentityProviderClient.ProviderSession providerSession) {
        String employeeId = claim(providerSession.claims(), "employee_id", "emp_id");
        var request = new ResolveIdentityRequest(
            "KEYCLOAK",
            providerSession.providerSubjectId(),
            source == IdentitySource.BANK_AD ? "BANK_EMPLOYEE" : "INSURER_REPRESENTATIVE",
            providerSession.username(),
            providerSession.email(),
            employeeId
        );
        return client.post().uri("/internal/v1/identities/resolve").body(request).retrieve()
            .body(ResolvedIdentity.class);
    }

    private static String claim(Map<String, Object> claims, String... names) {
        for (String name : names) {
            Object value = claims.get(name);
            if (value != null) return value.toString();
        }
        return null;
    }

    private record ResolveIdentityRequest(
        String provider,
        String providerSubject,
        String userType,
        String username,
        String email,
        String employeeId
    ) {}

    public record ResolvedIdentity(
        UUID businessUserId,
        String status,
        long policyVersion,
        String userType,
        String insurerCode
    ) {}
}
