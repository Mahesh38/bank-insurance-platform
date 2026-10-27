package com.bank.identity.authz.api;

import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCaller;
import com.bank.identity.authz.config.InternalAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Least-privilege check after inbound caller authentication (SUG-20261027-iah). */
@Component
public class PrivilegeGuard {

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;

  public PrivilegeGuard(InternalAuthProperties properties, ServiceErrors errors) {
    this.properties = properties;
    this.errors = errors;
  }

  public String require(HttpServletRequest request, String privilege, String operation) {
    String caller = InternalCaller.requireCaller(request, errors, operation);
    InternalCaller.requirePrivilege(properties.credentials(), caller, privilege, errors, operation);
    return caller;
  }
}
