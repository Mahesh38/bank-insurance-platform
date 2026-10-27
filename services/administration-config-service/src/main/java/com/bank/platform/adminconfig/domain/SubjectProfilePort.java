package com.bank.platform.adminconfig.domain;

import java.util.Set;

/** Authoritative subject facts for checker decisions — never trust request role headers. */
public interface SubjectProfilePort {

  SubjectProfile load(String businessUserId);

  record SubjectProfile(String userType, Set<String> roleCodes) {
    public SubjectProfile {
      roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
  }
}
