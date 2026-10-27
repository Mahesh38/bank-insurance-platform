package com.bank.common.security.internal;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/** Request helpers for authenticated internal callers and actor assertion. */
public final class InternalCaller {

  private InternalCaller() {}

  public static String requireCaller(
      HttpServletRequest request, ServiceErrors errors, String operation) {
    Object value = request.getAttribute(InternalCallerAuth.CALLER_ATTRIBUTE);
    if (!(value instanceof String caller) || caller.isBlank()) {
      throw errors
          .error(ErrorCodes.UNAUTHORIZED)
          .component("InternalCaller")
          .operation(operation)
          .reason("internal caller is not authenticated")
          .build();
    }
    return caller;
  }

  public static void requirePrivilege(
      InternalCallerCredentials credentials,
      String caller,
      String privilege,
      ServiceErrors errors,
      String operation) {
    if (!credentials.hasPrivilege(caller, privilege)) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("InternalCaller")
          .operation(operation)
          .reason("caller may not perform " + privilege)
          .build();
    }
  }

  public static UUID requireActorId(
      InternalCallerCredentials credentials,
      String caller,
      String actorHeader,
      ServiceErrors errors,
      String operation) {
    if (!credentials.mayAssertActor(caller)) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("InternalCaller")
          .operation(operation)
          .reason("caller may not assert X-Actor-Id")
          .build();
    }
    if (actorHeader == null || actorHeader.isBlank()) {
      throw errors
          .error(ErrorCodes.UNAUTHORIZED)
          .component("InternalCaller")
          .operation(operation)
          .reason("X-Actor-Id is required")
          .build();
    }
    try {
      return UUID.fromString(actorHeader.trim());
    } catch (IllegalArgumentException ex) {
      throw errors
          .error(ErrorCodes.UNAUTHORIZED)
          .component("InternalCaller")
          .operation(operation)
          .reason("X-Actor-Id is not a business-user id")
          .build();
    }
  }

  public static String requireActorIdString(
      InternalCallerCredentials credentials,
      String caller,
      String actorHeader,
      ServiceErrors errors,
      String operation) {
    return requireActorId(credentials, caller, actorHeader, errors, operation).toString();
  }
}
