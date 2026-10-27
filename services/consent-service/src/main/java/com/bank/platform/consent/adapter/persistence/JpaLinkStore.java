package com.bank.platform.consent.adapter.persistence;

import com.bank.platform.consent.domain.ConsentLink;
import com.bank.platform.consent.domain.LinkPorts;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaLinkStore implements LinkPorts.Links {

  private final EntityManager entities;
  private final ConsentLinkRepository links;
  private final ConsentLinkIssueRepository issues;

  public JpaLinkStore(
      EntityManager entities, ConsentLinkRepository links, ConsentLinkIssueRepository issues) {
    this.entities = entities;
    this.links = links;
    this.issues = issues;
  }

  @Override
  public void insert(ConsentLink link) {
    entities.persist(toEntity(link));
  }

  @Override
  public void update(ConsentLink link) {
    ConsentLinkEntity entity =
        links
            .findById(link.linkId())
            .orElseThrow(() -> new IllegalStateException("consent link missing"));
    entity.setState(link.state());
    entity.setDecidedAt(link.decidedAt());
    entity.setSourceIp(link.sourceIp());
    entity.setConsentId(link.consentId());
  }

  @Override
  public Optional<ConsentLink> findByHash(String tokenHash) {
    return links.findByTokenHash(tokenHash).map(JpaLinkStore::toLink);
  }

  @Override
  public Optional<ConsentLink> lockByHash(String tokenHash) {
    return links.lockByTokenHash(tokenHash).map(JpaLinkStore::toLink);
  }

  @Override
  public Optional<ConsentLink> pending(String assessmentId) {
    return links.pending(assessmentId).stream().findFirst().map(JpaLinkStore::toLink);
  }

  @Override
  public Optional<ConsentLink> latest(String assessmentId) {
    return links.latest(assessmentId).stream().findFirst().map(JpaLinkStore::toLink);
  }

  @Override
  public int countIssuedSince(String customerId, Instant since) {
    return issues.countSince(customerId, since);
  }

  @Override
  public void recordIssue(String issueId, String customerId, String assessmentId, Instant at) {
    ConsentLinkIssueEntity entity = new ConsentLinkIssueEntity();
    entity.setIssueId(issueId);
    entity.setCustomerId(customerId);
    entity.setAssessmentId(assessmentId);
    entity.setIssuedAt(at);
    entities.persist(entity);
  }

  private static ConsentLinkEntity toEntity(ConsentLink link) {
    ConsentLinkEntity entity = new ConsentLinkEntity();
    entity.setLinkId(link.linkId());
    entity.setTokenHash(link.tokenHash());
    entity.setCustomerId(link.customerId());
    entity.setLeadId(link.leadId());
    entity.setJourneyId(link.journeyId());
    entity.setAssessmentId(link.assessmentId());
    entity.setEncryptionKeyId(link.mobileEncKeyId());
    entity.setMobileEnc(link.mobileEnc());
    entity.setEmailEnc(link.emailEnc());
    entity.setMobileMasked(link.mobileMasked());
    entity.setEmailMasked(link.emailMasked());
    entity.setState(link.state());
    entity.setExpiresAt(link.expiresAt());
    entity.setIssuedAt(link.issuedAt());
    entity.setDecidedAt(link.decidedAt());
    entity.setSourceIp(link.sourceIp());
    entity.setConsentId(link.consentId());
    entity.setActorId(link.actorId());
    entity.setConfigVersionRef(link.configVersionRef());
    return entity;
  }

  private static ConsentLink toLink(ConsentLinkEntity entity) {
    return new ConsentLink(
        entity.getLinkId(),
        entity.getTokenHash(),
        entity.getCustomerId(),
        entity.getLeadId(),
        entity.getJourneyId(),
        entity.getAssessmentId(),
        entity.getEncryptionKeyId(),
        entity.getMobileEnc(),
        entity.getEmailEnc(),
        entity.getMobileMasked(),
        entity.getEmailMasked(),
        entity.getState(),
        entity.getExpiresAt(),
        entity.getIssuedAt(),
        entity.getDecidedAt(),
        entity.getSourceIp(),
        entity.getConsentId(),
        entity.getActorId(),
        entity.getConfigVersionRef());
  }
}
