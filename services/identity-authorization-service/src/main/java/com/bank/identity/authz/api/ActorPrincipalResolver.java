package com.bank.identity.authz.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
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
    if (!properties.mayAssertActor(caller)) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("ActorPrincipalResolver")
          .operation(operation)
          .reason("caller may not assert X-Actor-Id")
          .build();
    }
    if (actorHeader == null || actorHeader.isBlank()) {
      throw unauthenticatedActor(operation, "X-Actor-Id is required");
    }
    try {
      return UUID.fromString(actorHeader.trim());
    } catch (IllegalArgumentException exception) {
      throw unauthenticatedActor(operation, "X-Actor-Id is not a business-user id");
    }
  }

  private RuntimeException unauthenticatedActor(String operation, String reason) {
    return errors
        .error(ErrorCodes.UNAUTHORIZED)
        .component("ActorPrincipalResolver")
        .operation(operation)
        .reason(reason)
        .build();
  }
}
