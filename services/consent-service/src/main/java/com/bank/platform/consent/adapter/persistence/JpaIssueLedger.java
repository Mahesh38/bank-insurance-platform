package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.Ulids;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JpaIssueLedger implements ConsentPorts.IssueLedger {

  private final OtpIssueRepository issues;
  private final EntityManager entities;
  private final Clock clock;

  public JpaIssueLedger(OtpIssueRepository issues, EntityManager entities, Clock clock) {
    this.issues = issues;
    this.entities = entities;
    this.clock = clock;
  }

  @Override
  public int countSince(String customerId, String purpose, Instant since) {
    return issues.countSince(customerId, purpose, since);
  }

  @Override
  public void record(String customerId, String purpose, Instant at) {
    OtpIssueEntity entity = new OtpIssueEntity();
    entity.setIssueId(Ulids.mint(clock));
    entity.setCustomerId(customerId);
    entity.setPurpose(purpose);
    entity.setIssuedAt(at);
    entities.persist(entity);
  }
}
