package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.PublishedStatement;
import com.bank.platform.consent.domain.StatementRules;
import com.bank.platform.consent.domain.StatementStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStatementStore implements StatementStore {

  private final EntityManager em;

  public JpaStatementStore(EntityManager em) {
    this.em = em;
  }

  @Override
  public void insert(PublishedStatement statement) {
    if (find(statement.statementId(), statement.version(), statement.language()).isPresent()) {
      throw new ConsentRefusal(
          ConsentCodes.STATEMENT_VERSION_IMMUTABLE, "statement version is immutable");
    }
    em.persist(PublishedStatementEntity.from(statement));
    try {
      em.flush();
    } catch (PersistenceException ex) {
      throw new ConsentRefusal(
          ConsentCodes.STATEMENT_VERSION_IMMUTABLE, "statement version is immutable");
    }
  }

  @Override
  public Optional<PublishedStatement> find(String statementId, String version, String language) {
    PublishedStatementEntity entity =
        em.find(PublishedStatementEntity.class, new StatementKey(statementId, version, language));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(entity.toDomain());
  }

  @Override
  public boolean englishSourceExists(String statementId, String version) {
    return find(statementId, version, StatementRules.R0_LANGUAGE).isPresent();
  }
}
