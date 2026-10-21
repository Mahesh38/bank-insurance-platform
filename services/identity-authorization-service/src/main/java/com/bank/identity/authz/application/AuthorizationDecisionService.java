package com.bank.identity.authz.application;

import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationPolicyEvaluator;
import com.bank.identity.authz.persistence.AuthorizationFactsRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationDecisionService {

  private final AuthorizationFactsRepository factsRepository;
  private final AuthorizationPolicyEvaluator evaluator;
  private final Clock clock;

  public AuthorizationDecisionService(
      AuthorizationFactsRepository factsRepository, AuthorizationPolicyEvaluator evaluator) {
    this.factsRepository = factsRepository;
    this.evaluator = evaluator;
    this.clock = Clock.systemUTC();
  }

  public Decision decide(DecisionRequest request) {
    var now = clock.instant();
    return factsRepository
        .load(request.subjectId(), now)
        .map(facts -> evaluator.evaluate(facts, request, now))
        .orElseGet(() -> new Decision(false, "UNKNOWN_SUBJECT", "default-deny", 0L));
  }
}
