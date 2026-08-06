package com.bank.identity.authz.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessUserRepository extends JpaRepository<BusinessUserEntity, UUID> {
    Optional<BusinessUserEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
    boolean existsByUsername(String username);
}
