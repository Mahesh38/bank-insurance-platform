package com.bank.platform.lead.adapter.memory;

import com.bank.platform.lead.domain.LeadExceptionEvidencePort;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Test and local-only companion of {@link InMemoryLeadRepository} ({@code lead.store=memory}). */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "memory")
public class InMemoryLeadExceptionEvidenceStore implements LeadExceptionEvidencePort {

  private final List<StoredEvaluation> evaluations = new CopyOnWriteArrayList<>();
  private final List<Decision> decisions = new CopyOnWriteArrayList<>();

  @Override
  public void recordEvaluation(StoredEvaluation evaluation) {
    evaluations.add(evaluation);
  }

  @Override
  public Optional<StoredEvaluation> findEvaluation(String evaluationId) {
    return evaluations.stream().filter(e -> e.evaluationId().equals(evaluationId)).findFirst();
  }

  @Override
  public void recordDecision(Decision decision) {
    // Same guarantee as ux_lead_exc_decision_eval: one decision per evaluation.
    if (decisions.stream().anyMatch(d -> d.evaluationId().equals(decision.evaluationId()))) {
      throw new IllegalStateException("evaluation already decided: " + decision.evaluationId());
    }
    decisions.add(decision);
  }

  @Override
  public List<Decision> decisionsOf(String leadId) {
    return decisions.stream().filter(d -> d.leadId().equals(leadId)).toList();
  }

  public List<StoredEvaluation> evaluations() {
    return List.copyOf(evaluations);
  }
}
