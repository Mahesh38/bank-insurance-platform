package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.RetentionStore;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/** Insert-only retention anchor. A second, different anchor is a conflict. */
@Repository
public class JpaRetentionStore implements RetentionStore {

  private final EntityManager em;
  private final Clock clock;

  public JpaRetentionStore(EntityManager em, Clock clock) {
    this.em = em;
    this.clock = clock;
  }

  @Override
  public void anchor(String rootConsentId, Instant terminalAt, Instant retainUntil) {
    RetentionAnchorEntity existing = em.find(RetentionAnchorEntity.class, rootConsentId);
    if (existing != null) {
      if (Objects.equals(existing.terminalAt(), terminalAt)
          && Objects.equals(existing.retainUntil(), retainUntil)) {
        return;
      }
      throw new ConsentRefusal(ConsentCodes.CONFLICT, "retention anchor is immutable");
    }
    em.persist(new RetentionAnchorEntity(rootConsentId, terminalAt, retainUntil, clock.instant()));
  }
}
