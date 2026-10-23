package com.bank.platform.adminconfig.domain;

import com.bank.rules.engine.RuleSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * One version of one rule set (ADR-024 section 5). Maker-checker: a different actor holding the
 * role named by the document's classification approves; approval publishes. A published version
 * never changes again — a correction is a new version (SUIT-R07).
 *
 * <p>Whether the document's test cases pass is checked by the application service before {@link
 * #approve}; this aggregate only guards who may move it and from where.
 */
public final class RuleSetVersion {

  private final String id;
  private final String ruleSetId;
  private final String version;
  private final Instant createdAt;
  private String domain;
  private String scopeLob;
  private String scopeProductClass;
  private RuleSet.Classification classification;
  private RuleSetVersionStatus status;
  private String document;
  private String documentSha256;
  private OffsetDateTime effectiveFrom;
  private OffsetDateTime effectiveTo;
  private String maker;
  private String checker;
  private Instant approvedAt;
  private String decisionRemarks;

  private RuleSetVersion(Snapshot s) {
    this.id = s.id();
    this.ruleSetId = s.ruleSetId();
    this.version = s.version();
    this.domain = s.domain();
    this.scopeLob = s.scopeLob();
    this.scopeProductClass = s.scopeProductClass();
    this.classification = s.classification();
    this.status = s.status();
    this.document = s.document();
    this.documentSha256 = s.documentSha256();
    this.effectiveFrom = s.effectiveFrom();
    this.effectiveTo = s.effectiveTo();
    this.maker = s.maker();
    this.checker = s.checker();
    this.approvedAt = s.approvedAt();
    this.decisionRemarks = s.decisionRemarks();
    this.createdAt = s.createdAt();
  }

  /** A new DRAFT from a document that has already passed {@code RuleSetParser}. */
  public static RuleSetVersion draft(
      String id, RuleSet parsed, String documentJson, String maker, Instant now) {
    Objects.requireNonNull(maker, "maker");
    return new RuleSetVersion(
        new Snapshot(
            id,
            parsed.ruleSetId(),
            parsed.version(),
            parsed.domain(),
            parsed.scope().lob(),
            parsed.scope().productClass(),
            parsed.classification(),
            RuleSetVersionStatus.DRAFT,
            documentJson,
            parsed.documentSha256(),
            parsed.effectiveFrom(),
            parsed.effectiveTo(),
            maker,
            null,
            null,
            null,
            now));
  }

  public static RuleSetVersion restore(Snapshot s) {
    return new RuleSetVersion(s);
  }

  /** Replaces the document of a DRAFT or REJECTED version; it becomes DRAFT again. */
  public void replaceDocument(RuleSet parsed, String documentJson, String actor) {
    require(Set.of(RuleSetVersionStatus.DRAFT, RuleSetVersionStatus.REJECTED), "edit");
    if (!parsed.ruleSetId().equals(ruleSetId) || !parsed.version().equals(version)) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.ILLEGAL_TRANSITION,
          "document ruleSetId/version must stay " + ruleSetId + "@" + version);
    }
    this.domain = parsed.domain();
    this.scopeLob = parsed.scope().lob();
    this.scopeProductClass = parsed.scope().productClass();
    this.classification = parsed.classification();
    this.document = documentJson;
    this.documentSha256 = parsed.documentSha256();
    this.effectiveFrom = parsed.effectiveFrom();
    this.effectiveTo = parsed.effectiveTo();
    this.maker = actor;
    this.checker = null;
    this.decisionRemarks = null;
    this.status = RuleSetVersionStatus.DRAFT;
  }

  public void submit() {
    require(Set.of(RuleSetVersionStatus.DRAFT), "submit");
    this.status = RuleSetVersionStatus.PENDING_APPROVAL;
  }

  public void approve(String checkerId, Set<String> roles, String remarks, Instant now) {
    checkDecision(checkerId, roles);
    this.status = RuleSetVersionStatus.PUBLISHED;
    this.checker = checkerId;
    this.approvedAt = now;
    this.decisionRemarks = remarks;
  }

  public void reject(String checkerId, Set<String> roles, String remarks, Instant now) {
    checkDecision(checkerId, roles);
    if (remarks == null || remarks.isBlank()) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.REMARKS_REQUIRED, "rejection needs remarks");
    }
    this.status = RuleSetVersionStatus.REJECTED;
    this.checker = checkerId;
    this.approvedAt = null;
    this.decisionRemarks = remarks;
  }

  private void checkDecision(String checkerId, Set<String> roles) {
    require(Set.of(RuleSetVersionStatus.PENDING_APPROVAL), "decide");
    if (checkerId == null || checkerId.equals(maker)) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.MAKER_CHECKER, "checker must differ from maker");
    }
    String role = classification.approverRole();
    if (roles == null || !roles.contains(role)) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.ROLE_REQUIRED,
          classification + " rule sets need a checker holding " + role);
    }
  }

  private void require(Set<RuleSetVersionStatus> allowed, String step) {
    if (!allowed.contains(status)) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.ILLEGAL_TRANSITION,
          "cannot " + step + " " + ruleSetId + "@" + version + " in status " + status);
    }
  }

  /** In force at {@code asOf}: published, started, not ended. */
  public boolean inForceAt(OffsetDateTime asOf) {
    return status == RuleSetVersionStatus.PUBLISHED
        && !asOf.isBefore(effectiveFrom)
        && (effectiveTo == null || asOf.isBefore(effectiveTo));
  }

  public Snapshot snapshot() {
    return new Snapshot(
        id,
        ruleSetId,
        version,
        domain,
        scopeLob,
        scopeProductClass,
        classification,
        status,
        document,
        documentSha256,
        effectiveFrom,
        effectiveTo,
        maker,
        checker,
        approvedAt,
        decisionRemarks,
        createdAt);
  }

  public String id() {
    return id;
  }

  public String ruleSetId() {
    return ruleSetId;
  }

  public String version() {
    return version;
  }

  public RuleSetVersionStatus status() {
    return status;
  }

  public String document() {
    return document;
  }

  /** Persisted state; also the read model. */
  public record Snapshot(
      String id,
      String ruleSetId,
      String version,
      String domain,
      String scopeLob,
      String scopeProductClass,
      RuleSet.Classification classification,
      RuleSetVersionStatus status,
      String document,
      String documentSha256,
      OffsetDateTime effectiveFrom,
      OffsetDateTime effectiveTo,
      String maker,
      String checker,
      Instant approvedAt,
      String decisionRemarks,
      Instant createdAt) {}
}
