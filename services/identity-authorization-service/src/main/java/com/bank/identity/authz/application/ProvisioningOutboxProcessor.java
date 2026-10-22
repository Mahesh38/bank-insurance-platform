package com.bank.identity.authz.application;

import com.bank.identity.authz.application.port.IdentityProvisioningPort;
import com.bank.identity.authz.application.port.IdentityProvisioningPort.ProvisionRequest;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "identity.provisioning.enabled", havingValue = "true")
public class ProvisioningOutboxProcessor {

  private final JdbcClient jdbc;
  private final BusinessUserRepository users;
  private final IdentityProvisioningPort provider;

  public ProvisioningOutboxProcessor(
      JdbcClient jdbc, BusinessUserRepository users, IdentityProvisioningPort provider) {
    this.jdbc = jdbc;
    this.users = users;
    this.provider = provider;
  }

  @Transactional
  public void provision(UUID eventId, UUID userId) {
    var user =
        users
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("Provisioning target no longer exists"));
    if (user.getProviderSubject() == null) {
      user.markProvisioned(
          provider.provision(
              new ProvisionRequest(
                  user.getId(),
                  user.getUsername(),
                  user.getEmail(),
                  user.getFirstName(),
                  user.getLastName(),
                  user.getInsurerCode())));
    }
    jdbc.sql(
            """
            update outbox_event
               set published_at = :now, attempts = attempts + 1, last_error = null,
                   claimed_until = null
             where id = :id and published_at is null
            """)
        .param("now", Instant.now())
        .param("id", eventId)
        .update();
  }
}
