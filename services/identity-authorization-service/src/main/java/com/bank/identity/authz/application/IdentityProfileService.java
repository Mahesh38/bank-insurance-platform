package com.bank.identity.authz.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.AuthorizationFactsRepository;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trusted subject profile for east-west callers (SUG-20261027-iah). Role codes come from {@code
 * user_role}, not permission expansion.
 */
@Service
public class IdentityProfileService {

  private final BusinessUserRepository users;
  private final AuthorizationFactsRepository facts;
  private final ServiceErrors errors;
  private final Clock clock;

  public IdentityProfileService(
      BusinessUserRepository users,
      AuthorizationFactsRepository facts,
      ServiceErrors errors,
      Clock clock) {
    this.users = users;
    this.facts = facts;
    this.errors = errors;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public SubjectProfile load(UUID subjectId) {
    BusinessUserEntity user =
        users
            .findById(subjectId)
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component("IdentityProfileService")
                        .operation("load")
                        .reason("subject not found")
                        .build());
    Instant now = clock.instant();
    Set<String> roleCodes = facts.findRoleCodes(subjectId, now);
    return new SubjectProfile(user.getId(), user.getUserType(), roleCodes, user.getStatus());
  }

  public record SubjectProfile(
      UUID subjectId, UserType userType, Set<String> roleCodes, UserStatus status) {

    public SubjectProfile {
      roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
  }
}
