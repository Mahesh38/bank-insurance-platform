package com.bank.identity.authz.api;

import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCaller;
import com.bank.identity.authz.config.InternalAuthProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Turns {@code X-Actor-Id} into an authenticated maker / checker. The header is only believed when
 * the service key identified a caller allowed to assert a human actor — the BFF, which takes it
 * from a vault-resolved session. Any other authenticated caller (for example lead-service, which
 * only asks for decisions) is refused, so a service key alone cannot approve a partner.
 */
@Component
public class ActorPrincipalResolver {

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;

  public ActorPrincipalResolver(InternalAuthProperties properties, ServiceErrors errors) {
    this.properties = properties;
    this.errors = errors;
  }

  public UUID requireActor(String caller, String actorHeader, String operation) {
    return InternalCaller.requireActorId(
        properties.credentials(), caller, actorHeader, errors, operation);
  }
}
