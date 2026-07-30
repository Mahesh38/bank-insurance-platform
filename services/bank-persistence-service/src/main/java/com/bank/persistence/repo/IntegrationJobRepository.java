package com.bank.persistence.repo;

import com.bank.persistence.entity.IntegrationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationJobRepository extends JpaRepository<IntegrationJobEntity, String> {
}
