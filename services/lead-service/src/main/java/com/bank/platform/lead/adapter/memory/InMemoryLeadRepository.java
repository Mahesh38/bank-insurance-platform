package com.bank.platform.lead.adapter.memory;

import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadRepository;
import com.bank.platform.lead.domain.ProductClass;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryLeadRepository implements LeadRepository {

    private final ConcurrentHashMap<String, Lead> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Lead lead) {
        byId.put(lead.leadId(), lead);
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
    public Optional<Lead> findUnfinished(String creatorPrincipalId, String customerId, ProductClass productClass) {
        return byId.values().stream()
            .filter(lead -> creatorPrincipalId.equals(lead.createdByPrincipalId()))
            .filter(lead -> customerId.equals(lead.customerId()))
            .filter(lead -> productClass == lead.productClass())
            .filter(Lead::unfinished)
            .findFirst();
    }
}
