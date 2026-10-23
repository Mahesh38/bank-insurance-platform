package com.bank.identity.authz.persistence;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessUserRepository extends JpaRepository<BusinessUserEntity, UUID> {
  Optional<BusinessUserEntity> findByProviderAndProviderSubject(
      String provider, String providerSubject);

  boolean existsByUsername(String username);

  Optional<BusinessUserEntity> findByEmployeeIdAndUserType(String employeeId, UserType userType);

  Optional<BusinessUserEntity> findByEmailIgnoreCaseAndUserType(String email, UserType userType);
}
