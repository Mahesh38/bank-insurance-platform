package com.bank.platform.lead.domain;

import java.util.Set;

/**
 * Trusted subject attributes from identity-authorization (SUG-20261027-iah). Lead must not believe
 * {@code X-Actor-Type} / {@code X-Actor-Roles} request headers.
 */
public interface SubjectProfilePort {

  SubjectProfile load(String businessUserId);

  record SubjectProfile(String userType, Set<String> roleCodes) {

    public SubjectProfile {
      roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
  }
}
