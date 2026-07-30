package com.bank.insurance.persistence.persistence.repo;

import com.bank.insurance.persistence.persistence.entity.IntegrationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationJobRepository extends JpaRepository<IntegrationJobEntity, String> {
}
