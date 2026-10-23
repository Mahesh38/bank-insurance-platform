package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRow;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaConsentStore implements ConsentPorts.ConsentStore, ConsentPorts.ChallengeStore {

  private final EntityManager entities;
  private final ConsentRecordRepository consents;
  private final ChallengeRepository challenges;

  public JpaConsentStore(
      EntityManager entities, ConsentRecordRepository consents, ChallengeRepository challenges) {
    this.entities = entities;
    this.consents = consents;
    this.challenges = challenges;
  }

  @Override
  public void insert(ConsentRow row) {
    entities.persist(RowMapping.toEntity(row));
  }

  @Override
  public Optional<ConsentRow> head(String customerId, String purpose, String insurerCode) {
    return consents.heads(customerId, purpose, insurerCode).stream()
        .findFirst()
        .map(RowMapping::toRow);
  }

  @Override
  public Optional<ConsentRow> headOf(String consentId) {
    return consents.headOf(consentId).stream().findFirst().map(RowMapping::toRow);
  }

  @Override
  public List<ConsentRow> chain(String consentId) {
    return consents.findByConsentIdOrderByRecordVersionAsc(consentId).stream()
        .map(RowMapping::toRow)
        .toList();
  }

  @Override
  public List<String> chainIdsForCustomer(String customerId) {
    return consents.chainIdsForCustomer(customerId);
  }

  @Override
  public List<String> chainIdsForJourney(String journeyId) {
    return consents.chainIdsForJourney(journeyId);
  }

  @Override
  public List<ConsentRow> grantedPastValidity(Instant asOf) {
    return consents.grantedPastValidity(asOf).stream().map(RowMapping::toRow).toList();
  }

  @Override
  public void insert(ChallengeRow row) {
    entities.persist(RowMapping.toEntity(row));
  }

  @Override
  public Optional<ChallengeRow> find(String challengeId) {
    return challenges.findById(challengeId).map(RowMapping::toRow);
  }

  @Override
  public void update(ChallengeRow row) {
    entities.merge(RowMapping.toEntity(row));
  }

  @Override
  public List<ChallengeRow> stale(Instant cutoff) {
    return challenges
        .findByStateInAndOtpExpiresAtBefore(List.of("OTP_PENDING", "LOCKED"), cutoff)
        .stream()
        .map(RowMapping::toRow)
        .toList();
  }
}
