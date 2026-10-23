package com.bank.identity.authz.application.port;

import java.util.UUID;

/**
 * Outbound port to the identity provider. The application layer depends on this, not on the HTTP
 * adapter that implements it (ArchUnit: application must not depend on adapters).
 */
public interface IdentityProvisioningPort {

  /** Returns the provider subject id. Idempotent per {@code businessUserId} on the adapter side. */
  String provision(ProvisionRequest request);

  record ProvisionRequest(
      UUID businessUserId,
      String username,
      String email,
      String firstName,
      String lastName,
      String insurerCode) {}
}
