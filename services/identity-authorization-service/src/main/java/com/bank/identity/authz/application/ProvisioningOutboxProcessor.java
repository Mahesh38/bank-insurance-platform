package com.bank.identity.authz.application;

import com.bank.identity.authz.adapter.ProviderProvisioningClient;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "identity.provisioning.enabled", havingValue = "true")
public class ProvisioningOutboxProcessor {

    private final JdbcClient jdbc;
    private final BusinessUserRepository users;
    private final ProviderProvisioningClient provider;

    public ProvisioningOutboxProcessor(
        JdbcClient jdbc,
        BusinessUserRepository users,
        ProviderProvisioningClient provider
    ) {
        this.jdbc = jdbc;
        this.users = users;
        this.provider = provider;
    }

    @Transactional
    public void provision(UUID eventId, UUID userId) {
        var user = users.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Provisioning target no longer exists"));
        if (user.getProviderSubject() == null) {
            user.markProvisioned(provider.provision(user));
        }
        jdbc.sql("""
                update outbox_event
                   set published_at = :now, attempts = attempts + 1, last_error = null
                 where id = :id and published_at is null
                """)
            .param("now", Instant.now())
            .param("id", eventId)
            .update();
    }
}
