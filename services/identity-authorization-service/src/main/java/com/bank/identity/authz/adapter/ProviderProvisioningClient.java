package com.bank.identity.authz.adapter;

import com.bank.identity.authz.application.port.IdentityProvisioningPort;
import com.bank.identity.authz.config.ProviderAdapterProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProviderProvisioningClient implements IdentityProvisioningPort {

  private final RestClient client;

  public ProviderProvisioningClient(
      @Qualifier("authorizationRestClient") RestClient restClient,
      ProviderAdapterProperties properties) {
    this.client =
        restClient
            .mutate()
            .baseUrl(properties.baseUrl().toString())
            .defaultHeader("X-Internal-Service-Key", properties.internalKey())
            .build();
  }

  @Override
  public String provision(ProvisionRequest request) {
    var response =
        client
            .post()
            .uri("/internal/v1/identities")
            .body(
                new ProvisionIdentityRequest(
                    request.businessUserId().toString(),
                    request.username(),
                    request.email(),
                    request.firstName(),
                    request.lastName(),
                    request.insurerCode(),
                    true))
            .retrieve()
            .body(ProvisionIdentityResponse.class);
    if (response == null
        || response.providerSubjectId() == null
        || response.providerSubjectId().isBlank()) {
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
      boolean enabled) {}

  private record ProvisionIdentityResponse(String providerSubjectId) {}
}
