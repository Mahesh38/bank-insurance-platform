package com.bank.insurance.persistence.persistence.repo;

import com.bank.insurance.persistence.persistence.entity.IntegrationJobOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationJobOfferRepository extends JpaRepository<IntegrationJobOfferEntity, String> {

    List<IntegrationJobOfferEntity> findByJobId(String jobId);
}
