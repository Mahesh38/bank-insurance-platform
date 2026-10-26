package com.bank.persistence.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationJobOfferRepository
    extends JpaRepository<IntegrationJobOfferEntity, String> {

  List<IntegrationJobOfferEntity> findByJobId(String jobId);
}
