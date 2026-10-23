package com.bank.platform.lead.adapter.memory;

import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadRepository;
import com.bank.platform.lead.domain.ProductClass;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Test and local-only store. Selected with {@code lead.store=memory}; data is lost on restart. */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "memory")
public class InMemoryLeadRepository implements LeadRepository {

  private final ConcurrentHashMap<String, Lead> byId = new ConcurrentHashMap<>();

  @Override
  public void save(Lead lead) {
    byId.put(lead.leadId(), lead);
    lead.markPersisted(lead.version() + 1);
  }

  @Override
  public Optional<Lead> findById(String leadId) {
    return Optional.ofNullable(byId.get(leadId));
  }

  @Override
  public List<Lead> findVisibleTo(String principalId) {
    List<Lead> matches = new ArrayList<>();
    for (Lead lead : byId.values()) {
      if (lead.visibleTo(principalId)) {
        matches.add(lead);
      }
    }
    matches.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
    return List.copyOf(matches);
  }

  @Override
  public Optional<Lead> findUnfinished(
      String creatorPrincipalId, String customerId, ProductClass productClass) {
    return byId.values().stream()
        .filter(lead -> creatorPrincipalId.equals(lead.createdByPrincipalId()))
        .filter(lead -> customerId.equals(lead.customerId()))
        .filter(lead -> productClass == lead.productClass())
        .filter(Lead::unfinished)
        .findFirst();
  }

  @Override
  public List<Lead> lockOverdue(Instant now, int limit) {
    return byId.values().stream()
        .filter(lead -> lead.overdue(now))
        .sorted((a, b) -> a.expiresAt().compareTo(b.expiresAt()))
        .limit(limit)
        .toList();
  }
}
