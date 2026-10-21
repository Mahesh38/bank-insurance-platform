package com.bank.identity.authz.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityResolutionService {

  private final BusinessUserRepository users;
  private final ServiceErrors errors;

  public IdentityResolutionService(BusinessUserRepository users, ServiceErrors errors) {
    this.users = users;
    this.errors = errors;
  }

  @Transactional
  public BusinessUserEntity resolve(ResolveIdentityCommand command) {
    return users
        .findByProviderAndProviderSubject(command.provider(), command.providerSubject())
        .map(
            existing -> {
              existing.refreshProfile(command.username(), command.email(), command.employeeId());
              return existing;
            })
        .orElseGet(() -> createBankEmployee(command));
  }

  private BusinessUserEntity createBankEmployee(ResolveIdentityCommand command) {
    if (command.userType() != UserType.BANK_EMPLOYEE) {
      // A typed 403 the BFF can show, not an unmapped 500 (SUG-20261021-gap).
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("IdentityResolutionService")
          .operation("resolve")
          .reason("Partner identity must be approved before first login")
          .build();
    }
    return users.save(
        BusinessUserEntity.bankEmployee(
            command.provider(),
            command.providerSubject(),
            command.username(),
            command.email(),
            command.employeeId()));
  }

  public record ResolveIdentityCommand(
      String provider,
      String providerSubject,
      UserType userType,
      String username,
      String email,
      String employeeId) {}
}
