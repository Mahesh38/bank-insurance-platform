package com.bank.platform.integration.hub.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCaller;
import com.bank.platform.integration.hub.config.InternalAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Opaque X-Actor-Id from actor-callers (same posture as lead-service). */
@Component
public class ActorGuard {

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;

  public ActorGuard(InternalAuthProperties properties, ServiceErrors errors) {
    this.properties = properties;
    this.errors = errors;
  }

  public String requireCaller(HttpServletRequest request, String operation) {
    return InternalCaller.requireCaller(request, errors, operation);
  }

  public String requireActorId(HttpServletRequest request, String actorHeader, String operation) {
    String caller = requireCaller(request, operation);
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
