package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ChangeClass;
import com.bank.platform.consent.domain.PublishedStatement;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "published_statement")
public class PublishedStatementEntity {

  @EmbeddedId private StatementKey id;

  @Column(name = "statement_text", nullable = false, length = 8000)
  private String statementText;

  @Column(name = "text_hash", nullable = false, length = 64)
  private String textHash;

  @Column(name = "change_class", nullable = false, length = 8)
  private String changeClass;

  @Column(name = "classified_by", nullable = false, length = 64)
  private String classifiedBy;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  protected PublishedStatementEntity() {}

  public static PublishedStatementEntity from(PublishedStatement statement) {
    PublishedStatementEntity entity = new PublishedStatementEntity();
    entity.id =
        new StatementKey(statement.statementId(), statement.version(), statement.language());
    entity.statementText = statement.text();
    entity.textHash = statement.textHash();
    entity.changeClass = statement.changeClass().name();
    entity.classifiedBy = statement.classifiedBy();
    entity.publishedAt = statement.publishedAt();
    return entity;
  }

  public PublishedStatement toDomain() {
    return new PublishedStatement(
        id.statementId(),
        id.version(),
        statementText,
        textHash,
        id.language(),
        ChangeClass.parse(changeClass),
        classifiedBy,
        publishedAt);
  }
}
