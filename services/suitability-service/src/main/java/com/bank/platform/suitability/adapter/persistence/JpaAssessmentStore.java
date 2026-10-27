package com.bank.platform.suitability.adapter.persistence;

import com.bank.platform.suitability.domain.Assessment;
import com.bank.platform.suitability.domain.JourneyModel.Assessments;
import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAssessmentStore implements Assessments {

  private final EntityManager entities;
  private final AssessmentRepository assessments;
  private final MappingProductRepository products;

  public JpaAssessmentStore(
      EntityManager entities, AssessmentRepository assessments, MappingProductRepository products) {
    this.entities = entities;
    this.assessments = assessments;
    this.products = products;
  }

  @Override
  public void insert(Assessment assessment) {
    entities.persist(toEntity(assessment, new AssessmentEntity()));
  }

  @Override
  public void update(Assessment assessment) {
    AssessmentEntity entity =
        assessments
            .findById(assessment.getAssessmentId())
            .orElseThrow(() -> new IllegalStateException("suitability assessment missing"));
    toEntity(assessment, entity);
  }

  @Override
  public Optional<Assessment> lock(String id) {
    return assessments.lockById(id).map(this::toDomain);
  }

  @Override
  public boolean leadExists(String leadId) {
    return assessments.existsByLeadId(leadId);
  }

  private Assessment toDomain(AssessmentEntity entity) {
    Assessment assessment = new Assessment();
    assessment.setAssessmentId(entity.getAssessmentId());
    assessment.setLeadId(entity.getLeadId());
    assessment.setJourneyId(entity.getJourneyId());
    assessment.setCustomerId(entity.getCustomerId());
    assessment.setProductType(entity.getProductType());
    assessment.setProposerName(entity.getProposerName());
    assessment.setProposerDob(entity.getProposerDob());
    assessment.setStatus(entity.getStatus());
    assessment.setLastScreen(entity.getLastScreen());
    assessment.setEncryptionKeyId(entity.getEncryptionKeyId());
    assessment.setMobileEnc(entity.getMobileEnc());
    assessment.setEmailEnc(entity.getEmailEnc());
    assessment.setMobileMasked(entity.getMobileMasked());
    assessment.setEmailMasked(entity.getEmailMasked());
    assessment.setSpActorId(entity.getSpActorId());
    assessment.setRmActorId(entity.getRmActorId());
    assessment.setLockActorId(entity.getLockActorId());
    assessment.setLockUntil(entity.getLockUntil());
    assessment.setPremiumInr(entity.getPremiumInr());
    assessment.setPremiumFrequency(entity.getPremiumFrequency());
    assessment.setPremiumPayingTerm(entity.getPremiumPayingTerm());
    assessment.setPolicyTerm(entity.getPolicyTerm());
    assessment.setDerivationRuleId(entity.getDerivationRuleId());
    assessment.setConsentState(entity.getConsentState());
    assessment.setAwaitingConsent(entity.isAwaitingConsent());
    assessment.setMappingVersion(entity.getMappingVersion());
    assessment.setMappingRunNo(entity.getMappingRunNo());
    assessment.setShowProducts(entity.isShowProducts());
    assessment.setSelectedProductId(entity.getSelectedProductId());
    assessment.setBiReference(entity.getBiReference());
    assessment.setResponseVersion(entity.getResponseVersion());
    assessment.setAnswers(entity.getAnswers());
    assessment.setNotice(entity.getNotice());
    assessment.setCreatedBy(entity.getCreatedBy());
    assessment.setCreatedAt(entity.getCreatedAt());
    assessment.setUpdatedBy(entity.getUpdatedBy());
    assessment.setUpdatedAt(entity.getUpdatedAt());
    if (entity.isShowProducts()) {
      assessment.setProducts(
          products.forRun(entity.getAssessmentId(), entity.getMappingRunNo()).stream()
              .map(
                  row ->
                      new ProductMatch(
                          row.getProductId(),
                          row.getInsurerCode(),
                          row.getProductName(),
                          row.getProductType()))
              .toList());
    }
    return assessment;
  }

  private static AssessmentEntity toEntity(Assessment assessment, AssessmentEntity entity) {
    entity.setAssessmentId(assessment.getAssessmentId());
    entity.setLeadId(assessment.getLeadId());
    entity.setJourneyId(assessment.getJourneyId());
    entity.setCustomerId(assessment.getCustomerId());
    entity.setProductType(assessment.getProductType());
    entity.setProposerName(assessment.getProposerName());
    entity.setProposerDob(assessment.getProposerDob());
    entity.setStatus(assessment.getStatus());
    entity.setLastScreen(assessment.getLastScreen());
    entity.setEncryptionKeyId(assessment.getEncryptionKeyId());
    entity.setMobileEnc(assessment.getMobileEnc());
    entity.setEmailEnc(assessment.getEmailEnc());
    entity.setMobileMasked(assessment.getMobileMasked());
    entity.setEmailMasked(assessment.getEmailMasked());
    entity.setSpActorId(assessment.getSpActorId());
    entity.setRmActorId(assessment.getRmActorId());
    entity.setLockActorId(assessment.getLockActorId());
    entity.setLockUntil(assessment.getLockUntil());
    entity.setPremiumInr(assessment.getPremiumInr());
    entity.setPremiumFrequency(assessment.getPremiumFrequency());
    entity.setPremiumPayingTerm(assessment.getPremiumPayingTerm());
    entity.setPolicyTerm(assessment.getPolicyTerm());
    entity.setDerivationRuleId(assessment.getDerivationRuleId());
    entity.setConsentState(assessment.getConsentState());
    entity.setAwaitingConsent(assessment.isAwaitingConsent());
    entity.setMappingVersion(assessment.getMappingVersion());
    entity.setMappingRunNo(assessment.getMappingRunNo());
    entity.setShowProducts(assessment.isShowProducts());
    entity.setSelectedProductId(assessment.getSelectedProductId());
    entity.setBiReference(assessment.getBiReference());
    entity.setResponseVersion(assessment.getResponseVersion());
    entity.setAnswers(assessment.getAnswers());
    entity.setNotice(assessment.getNotice());
    entity.setCreatedBy(assessment.getCreatedBy());
    entity.setCreatedAt(assessment.getCreatedAt());
    entity.setUpdatedBy(assessment.getUpdatedBy());
    entity.setUpdatedAt(assessment.getUpdatedAt());
    return entity;
  }
}
