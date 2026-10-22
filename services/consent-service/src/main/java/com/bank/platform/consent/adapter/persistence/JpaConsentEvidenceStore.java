package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentChains;
import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentEvidence;
import com.bank.platform.consent.domain.ConsentEvidenceStore;
import com.bank.platform.consent.domain.ConsentRefusal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Insert-only evidence. There is no update and no delete on this store. */
@Repository
public class JpaConsentEvidenceStore implements ConsentEvidenceStore {

  private final EntityManager em;

  public JpaConsentEvidenceStore(EntityManager em) {
    this.em = em;
  }

  @Override
  public void append(ConsentEvidence evidence) {
    if (em.find(ConsentEvidenceEntity.class, evidence.evidenceId()) != null) {
      throw new ConsentRefusal(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "evidence write failed");
    }
    em.persist(ConsentEvidenceEntity.from(evidence));
    try {
      em.flush();
    } catch (PersistenceException ex) {
      throw new ConsentRefusal(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "evidence write failed");
    }
  }

  @Override
  public List<ConsentEvidence> chain(String rootConsentId) {
    return em
        .createQuery(
            "select e from ConsentEvidenceEntity e where e.rootConsentId = :root order by e.recordVersion",
            ConsentEvidenceEntity.class)
        .setParameter("root", rootConsentId)
        .getResultList()
        .stream()
        .map(ConsentEvidenceEntity::toDomain)
        .toList();
  }

  @Override
  public List<ConsentEvidence> byJourney(String journeyId) {
    return em
        .createQuery(
            "select e from ConsentEvidenceEntity e where e.journeyId = :journeyId order by e.capturedAt",
            ConsentEvidenceEntity.class)
        .setParameter("journeyId", journeyId)
        .getResultList()
        .stream()
        .map(ConsentEvidenceEntity::toDomain)
        .toList();
  }

  @Override
  public List<ConsentEvidence> byCustomer(String customerId) {
    return em
        .createQuery(
            "select e from ConsentEvidenceEntity e where e.customerId = :customerId order by e.capturedAt",
            ConsentEvidenceEntity.class)
        .setParameter("customerId", customerId)
        .getResultList()
        .stream()
        .map(ConsentEvidenceEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<ConsentEvidence> head(
      String customerId, ConsentCode code, String counterpartyId) {
    return Optional.ofNullable(
        ConsentChains.selectHead(byCustomer(customerId), code, counterpartyId));
  }
}
