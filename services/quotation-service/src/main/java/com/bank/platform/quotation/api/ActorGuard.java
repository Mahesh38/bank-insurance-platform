package com.bank.platform.quotation.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCaller;
import com.bank.platform.quotation.config.InternalAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Accepts X-Actor-Id only from actor-callers. Opaque principal ids are allowed —
 * identity-authorization enforces UUID shape when it is the authority.
 */
@Component
public class ActorGuard {

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;

  public ActorGuard(InternalAuthProperties properties, ServiceErrors errors) {
    this.properties = properties;
    this.errors = errors;
  }

  public String requireActorId(HttpServletRequest request, String actorHeader, String operation) {
    String caller = InternalCaller.requireCaller(request, errors, operation);
    if (!properties.credentials().mayAssertActor(caller)) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("ActorGuard")
          .operation(operation)
          .reason("caller may not assert X-Actor-Id")
          .build();
    }
    if (actorHeader == null || actorHeader.isBlank()) {
      throw errors
          .error(ErrorCodes.UNAUTHORIZED)
          .component("ActorGuard")
          .operation(operation)
          .reason("X-Actor-Id is required")
          .build();
    }
    return actorHeader.trim();
  }
}
