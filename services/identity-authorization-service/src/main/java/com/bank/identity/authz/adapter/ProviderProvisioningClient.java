package com.bank.identity.authz.adapter;

import com.bank.identity.authz.config.ProviderAdapterProperties;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProviderProvisioningClient {

    private final RestClient client;

    public ProviderProvisioningClient(RestClient restClient, ProviderAdapterProperties properties) {
        this.client = restClient.mutate().baseUrl(properties.baseUrl().toString()).build();
    }

    public String provision(BusinessUserEntity user) {
        var response = client.post()
            .uri("/internal/v1/identities")
            .body(new ProvisionIdentityRequest(
                user.getId().toString(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getInsurerCode(), true))
            .retrieve()
            .body(ProvisionIdentityResponse.class);
        if (response == null || response.providerSubjectId() == null || response.providerSubjectId().isBlank()) {
            throw new IllegalStateException("Identity provider returned no subject identifier");
        }
        return response.providerSubjectId();
    }

    private record ProvisionIdentityRequest(
        String businessUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        String insurerCode,
        boolean enabled
    ) {}

    private record ProvisionIdentityResponse(String providerSubjectId) {}
}
