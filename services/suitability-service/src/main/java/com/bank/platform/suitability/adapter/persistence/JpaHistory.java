package com.bank.platform.suitability.adapter.persistence;

import com.bank.platform.suitability.domain.JourneyModel.History;
import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import com.bank.platform.suitability.domain.Ulids;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaHistory implements History {

  private final EntityManager entities;
  private final Clock clock;

  public JpaHistory(EntityManager entities, Clock clock) {
    this.entities = entities;
    this.clock = clock;
  }

  @Override
  public void answer(
      String assessmentId, String screen, String answers, String actorId, Instant at) {
    AnswerAuditEntity entity = new AnswerAuditEntity();
    entity.setAuditId(Ulids.mint(clock));
    entity.setAssessmentId(assessmentId);
    entity.setScreen(screen);
    entity.setAnswers(answers);
    entity.setActorId(actorId);
    entity.setRecordedAt(at);
    entities.persist(entity);
  }

  @Override
  public void mapping(
      String assessmentId,
      int runNo,
      String version,
      String outcome,
      String actorId,
      Instant at,
      List<ProductMatch> products) {
    MappingRunEntity run = new MappingRunEntity();
    run.setRunId(Ulids.mint(clock));
    run.setAssessmentId(assessmentId);
    run.setRunNo(runNo);
    run.setRuleVersion(version);
    run.setOutcome(outcome);
    run.setActorId(actorId);
    run.setRecordedAt(at);
    entities.persist(run);
    for (ProductMatch product : products) {
      MappingProductEntity row = new MappingProductEntity();
      row.setRunId(run.getRunId());
      row.setProductId(product.productId());
      row.setInsurerCode(product.insurerCode());
      row.setProductName(product.name());
      row.setProductType(product.productType());
      entities.persist(row);
    }
  }
}
