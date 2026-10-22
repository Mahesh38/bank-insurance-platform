package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.OtpChallengeStore;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOtpChallengeStore implements OtpChallengeStore {

  private final EntityManager em;

  public JpaOtpChallengeStore(EntityManager em) {
    this.em = em;
  }

  @Override
  public void save(OtpChallenge challenge) {
    OtpChallengeEntity existing = em.find(OtpChallengeEntity.class, challenge.challengeId());
    if (existing == null) {
      em.persist(OtpChallengeEntity.from(challenge));
      return;
    }
    existing.advance(challenge.attempts(), challenge.status().name());
  }

  @Override
  public Optional<OtpChallenge> find(String challengeId) {
    OtpChallengeEntity entity = em.find(OtpChallengeEntity.class, challengeId);
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(entity.toDomain());
  }

  @Override
  public int issuesSince(String customerId, ConsentCode code, Instant since) {
    Long count =
        em.createQuery(
                "select count(c) from OtpChallengeEntity c "
                    + "where c.customerId = :customerId and c.consentCode = :code and c.createdAt >= :since",
                Long.class)
            .setParameter("customerId", customerId)
            .setParameter("code", code.wire())
            .setParameter("since", since)
            .getSingleResult();
    return count.intValue();
  }
}
