package com.bank.platform.lead.domain;

import java.util.List;
import java.util.Optional;

public interface LeadRepository {

  void save(Lead lead);

  Optional<Lead> findById(String leadId);

  List<Lead> findVisibleTo(String principalId);

  Optional<Lead> findUnfinished(
      String creatorPrincipalId, String customerId, ProductClass productClass);
}
