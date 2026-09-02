package com.bank.workforce.bff.client;

import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.session.SessionModels.IdentitySource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
public class IdentityAuthorizationClient {

    private static final String DOWNSTREAM = "authz";

    private final RestClient client;
    private final ProblemJsonReader problemJsonReader;
    private final ServiceErrors serviceErrors;

    public IdentityAuthorizationClient(RestClient restClient, DownstreamProperties properties,
                                       ProblemJsonReader problemJsonReader,
                                       ServiceErrors serviceErrors) {
        this.client = restClient.mutate().baseUrl(properties.authorizationServiceBaseUrl().toString()).build();
        this.problemJsonReader = problemJsonReader;
        this.serviceErrors = serviceErrors;
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
        try {
            return client.post().uri("/internal/v1/identities/resolve").body(request).retrieve()
                .body(ResolvedIdentity.class);
        } catch (RestClientResponseException ex) {
            // The authorization service's refusal is the one the RM can act on. Losing it here —
            // as a bare RestClientResponseException reaching Spring's default 500 — is how a
            // "you are not permitted" becomes an unattributable server error on the RM's screen.
            throw ErrorPropagation
                .from(problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
                .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
                .calling(DOWNSTREAM, "resolveIdentity")
                .causedBy(ex)
                .toException();
        }
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
