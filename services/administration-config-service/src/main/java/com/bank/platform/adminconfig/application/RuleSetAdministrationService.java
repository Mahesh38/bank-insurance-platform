package com.bank.platform.adminconfig.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.adminconfig.domain.OutboxPort;
import com.bank.platform.adminconfig.domain.RuleSetLifecycleException;
import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.bank.platform.adminconfig.domain.RuleSetVersionRepository;
import com.bank.platform.adminconfig.domain.RuleSetVersionStatus;
import com.bank.platform.adminconfig.domain.Ulid;
import com.bank.platform.adminconfig.domain.VersionResolution;
import com.bank.rules.engine.RuleEngine;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetValidationException;
import com.bank.rules.engine.TestCaseRunner;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-set lifecycle (ADR-024 section 5): draft, submit, approve-and-publish (maker-checker, role
 * by classification, every test case must pass), reject, simulate, list, resolve. Publishing writes
 * {@code RuleSetPublished} to the outbox in the same transaction (ADR-025).
 */
@Service
public class RuleSetAdministrationService {

  static final String COMPONENT = "RuleSetAdministrationService";

  private final RuleSetVersionRepository repository;
  private final OutboxPort outbox;
  private final ServiceErrors errors;
  private final Clock clock;

  public RuleSetAdministrationService(
      RuleSetVersionRepository repository, OutboxPort outbox, ServiceErrors errors, Clock clock) {
    this.repository = repository;
    this.outbox = outbox;
    this.errors = errors;
    this.clock = clock;
  }

  /** Actor as asserted by the caller; {@code roles} come from X-Actor-Roles until the PDP. */
  public record Actor(String id, Set<String> roles) {}

  public record SimulationResult(
      RuleSetVersion.Snapshot version, JsonNode evaluation, TestCaseRunner.Report testReport) {}

  @Transactional
  public RuleSetVersion createDraft(String documentJson, String maker) {
    RuleSet parsed = parse(documentJson, "createDraft");
    RuleSetVersion draft =
        RuleSetVersion.draft(Ulid.mint(clock), parsed, canonical(parsed), maker, clock.instant());
    try {
      repository.insert(draft);
    } catch (RuleSetVersionRepository.DuplicateVersionException e) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component(COMPONENT)
          .operation("createDraft")
          .reason(parsed.ruleSetId() + "@" + parsed.version() + " already exists")
          .cause(e)
          .build();
    }
    return draft;
  }

  @Transactional
  public RuleSetVersion replaceDraft(
      String ruleSetId, String version, String documentJson, String actor) {
    RuleSetVersion v = load(ruleSetId, version, "replaceDraft");
    RuleSet parsed = parse(documentJson, "replaceDraft");
    RuleSetVersionStatus before = v.status();
    lifecycle(() -> v.replaceDocument(parsed, canonical(parsed), actor), "replaceDraft");
    lifecycle(() -> repository.update(v, before), "replaceDraft");
    return v;
  }

  @Transactional
  public RuleSetVersion submit(String ruleSetId, String version, String actor) {
    RuleSetVersion v = load(ruleSetId, version, "submit");
    lifecycle(v::submit, "submit");
    lifecycle(() -> repository.update(v, RuleSetVersionStatus.DRAFT), "submit");
    return v;
  }

  /**
   * Checker approval publishes the version. Refused unless checker != maker, the checker holds the
   * classification's approver role, and every test case in the document passes.
   */
  @Transactional
  public RuleSetVersion approve(
      String ruleSetId, String version, Actor checker, String remarks, String correlationId) {
    RuleSetVersion v = load(ruleSetId, version, "approve");
    RuleSet parsed = RuleSetParser.parse(v.document());
    TestCaseRunner.Report report = TestCaseRunner.run(parsed);
    Instant now = clock.instant();
    lifecycle(() -> v.approve(checker.id(), checker.roles(), remarks, now), "approve");
    if (!report.allPassed()) {
      throw errors
          .error(AdminConfigErrorCodes.RULE_SET_TEST_FAILED)
          .component(COMPONENT)
          .operation("approve")
          .reason(
              ruleSetId + "@" + version + ": " + report.failed().size() + " test case(s) failed")
          .errors(
              report.failed().stream()
                  .map(
                      c ->
                          ServiceError.ofField(
                              AdminConfigErrorCodes.RULE_SET_TEST_FAILED,
                              String.join("; ", c.failures()),
                              "testCases." + c.name()))
                  .toList())
          .build();
    }
    lifecycle(() -> repository.update(v, RuleSetVersionStatus.PENDING_APPROVAL), "approve");
    outbox.append(
        RuleSetPublishedEvent.of(Ulid.mint(clock), v.snapshot(), checker.id(), now, correlationId));
    return v;
  }

  @Transactional
  public RuleSetVersion reject(String ruleSetId, String version, Actor checker, String remarks) {
    RuleSetVersion v = load(ruleSetId, version, "reject");
    lifecycle(() -> v.reject(checker.id(), checker.roles(), remarks, clock.instant()), "reject");
    lifecycle(() -> repository.update(v, RuleSetVersionStatus.PENDING_APPROVAL), "reject");
    return v;
  }

  /** Runs any inputs and every test case against any version, in any status. Changes nothing. */
  @Transactional(readOnly = true)
  public SimulationResult simulate(String ruleSetId, String version, JsonNode inputs) {
    RuleSetVersion v = load(ruleSetId, version, "simulate");
    RuleSet parsed = RuleSetParser.parse(v.document());
    JsonNode evaluation =
        inputs == null || inputs.isNull() || inputs.isMissingNode()
            ? null
            : RuleEngine.evaluate(parsed, inputs, clock.instant().toString()).toJson();
    return new SimulationResult(v.snapshot(), evaluation, TestCaseRunner.run(parsed));
  }

  @Transactional(readOnly = true)
  public List<RuleSetVersion> listVersions(String ruleSetId) {
    return repository.listVersions(ruleSetId);
  }

  @Transactional(readOnly = true)
  public RuleSetVersion get(String ruleSetId, String version) {
    return load(ruleSetId, version, "get");
  }

  /** The version in force; none is {@code CONFIGURATION_UNRESOLVABLE} — never a default. */
  @Transactional(readOnly = true)
  public RuleSetVersion resolve(
      String domain, String lob, String productClass, OffsetDateTime asOf) {
    OffsetDateTime at = asOf != null ? asOf : OffsetDateTime.now(clock);
    return VersionResolution.inForce(repository.listPublished(domain), lob, productClass, at)
        .orElseThrow(
            () ->
                errors
                    .error(ErrorCodes.CONFIGURATION_UNRESOLVABLE)
                    .component(COMPONENT)
                    .operation("resolve")
                    .reason(
                        "no published rule set for "
                            + domain
                            + "/"
                            + lob
                            + "/"
                            + productClass
                            + " at "
                            + at)
                    .build());
  }

  private RuleSetVersion load(String ruleSetId, String version, String operation) {
    return repository
        .find(ruleSetId, version)
        .orElseThrow(
            () ->
                errors
                    .error(ErrorCodes.RESOURCE_NOT_FOUND)
                    .component(COMPONENT)
                    .operation(operation)
                    .reason(ruleSetId + "@" + version + " not found")
                    .build());
  }

  private RuleSet parse(String documentJson, String operation) {
    try {
      return RuleSetParser.parse(documentJson);
    } catch (RuleSetValidationException e) {
      throw errors
          .error(AdminConfigErrorCodes.RULE_SET_INVALID)
          .component(COMPONENT)
          .operation(operation)
          .reason(e.errors().size() + " validation finding(s)")
          .errors(
              e.errors().stream()
                  .map(
                      f ->
                          ServiceError.ofField(
                              AdminConfigErrorCodes.RULE_SET_INVALID, f, f.split(":", 2)[0]))
                  .toList())
          .build();
    }
  }

  /** Stored as canonical JSON so documentSha256 can be recomputed from the column alone. */
  private static String canonical(RuleSet parsed) {
    return Values.canonicalString(parsed.document());
  }

  private void lifecycle(Runnable step, String operation) {
    try {
      step.run();
    } catch (RuleSetLifecycleException e) {
      String code =
          switch (e.kind()) {
            case ILLEGAL_TRANSITION -> ErrorCodes.ILLEGAL_TRANSITION;
            case MAKER_CHECKER -> AdminConfigErrorCodes.MAKER_CHECKER_VIOLATION;
            case ROLE_REQUIRED -> ErrorCodes.FORBIDDEN;
            case REMARKS_REQUIRED -> ErrorCodes.MISSING_REQUIRED_FIELD;
          };
      ServiceException ex =
          errors
              .error(code)
              .component(COMPONENT)
              .operation(operation)
              .reason(e.getMessage())
              .cause(e)
              .build();
      throw ex;
    }
  }

  static ObjectNode object() {
    return Values.mapper().createObjectNode();
  }

  static String write(JsonNode node) {
    try {
      return Values.mapper().writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
