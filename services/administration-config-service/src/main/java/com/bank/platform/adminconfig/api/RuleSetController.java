package com.bank.platform.adminconfig.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.adminconfig.application.RuleSetAdministrationService;
import com.bank.platform.adminconfig.application.RuleSetAdministrationService.Actor;
import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.bank.rules.engine.TestCaseRunner;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal rule-set API (ADR-024 section 5). Contract:
 * docs/platform/contracts/administration-config-service/openapi.yaml.
 *
 * <p>Approver roles come from {@code X-Actor-Roles} until the PDP decides this action (open item);
 * an approval without that header is refused — {@code X-Actor-Id} alone is never trusted for it.
 */
@RestController
public class RuleSetController {

  static final String BASE = "/internal/v1/rule-sets";

  private final RuleSetAdministrationService service;
  private final ServiceErrors errors;

  public RuleSetController(RuleSetAdministrationService service, ServiceErrors errors) {
    this.service = service;
    this.errors = errors;
  }

  @PostMapping(BASE + "/{ruleSetId}/versions")
  public ResponseEntity<VersionResponse> createDraft(
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String ruleSetId,
      @RequestBody JsonNode document) {
    if (!ruleSetId.equals(document.path("ruleSetId").asText())) {
      throw errors
          .error(ErrorCodes.INVALID_REQUEST)
          .component("RuleSetController")
          .operation("createDraft")
          .reason("path ruleSetId does not match the document")
          .build();
    }
    RuleSetVersion v = service.createDraft(document.toString(), actorId);
    return ResponseEntity.status(HttpStatus.CREATED).body(VersionResponse.full(v));
  }

  @PutMapping(BASE + "/{ruleSetId}/versions/{version}")
  public VersionResponse replaceDraft(
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String ruleSetId,
      @PathVariable String version,
      @RequestBody JsonNode document) {
    return VersionResponse.full(
        service.replaceDraft(ruleSetId, version, document.toString(), actorId));
  }

  @PostMapping(BASE + "/{ruleSetId}/versions/{version}:submit")
  public VersionResponse submit(
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String ruleSetId,
      @PathVariable String version) {
    return VersionResponse.summary(service.submit(ruleSetId, version, actorId));
  }

  @PostMapping(BASE + "/{ruleSetId}/versions/{version}:approve")
  public VersionResponse approve(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String roles,
      @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId,
      @PathVariable String ruleSetId,
      @PathVariable String version,
      @RequestBody(required = false) DecisionRequest body) {
    return VersionResponse.summary(
        service.approve(
            ruleSetId,
            version,
            checker(actorId, roles, "approve"),
            body == null ? null : body.remarks(),
            correlationId));
  }

  @PostMapping(BASE + "/{ruleSetId}/versions/{version}:reject")
  public VersionResponse reject(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(name = "X-Actor-Roles", required = false) String roles,
      @PathVariable String ruleSetId,
      @PathVariable String version,
      @RequestBody(required = false) DecisionRequest body) {
    return VersionResponse.summary(
        service.reject(
            ruleSetId,
            version,
            checker(actorId, roles, "reject"),
            body == null ? null : body.remarks()));
  }

  @PostMapping(BASE + "/{ruleSetId}/versions/{version}:simulate")
  public SimulationResponse simulate(
      @PathVariable String ruleSetId,
      @PathVariable String version,
      @RequestBody(required = false) SimulationRequest body) {
    var result = service.simulate(ruleSetId, version, body == null ? null : body.inputs());
    return new SimulationResponse(
        result.version().ruleSetId(),
        result.version().version(),
        result.version().status().name(),
        result.evaluation(),
        result.testReport().allPassed(),
        result.testReport().cases());
  }

  @GetMapping(BASE + "/{ruleSetId}/versions")
  public VersionPage list(@PathVariable String ruleSetId) {
    return new VersionPage(
        service.listVersions(ruleSetId).stream().map(VersionResponse::summary).toList());
  }

  @GetMapping(BASE + "/{ruleSetId}/versions/{version}")
  public VersionResponse get(@PathVariable String ruleSetId, @PathVariable String version) {
    return VersionResponse.full(service.get(ruleSetId, version));
  }

  @GetMapping(BASE + ":resolve")
  public VersionResponse resolve(
      @RequestParam String domain,
      @RequestParam(required = false) String lob,
      @RequestParam(required = false) String productClass,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime asOf) {
    return VersionResponse.full(service.resolve(domain, lob, productClass, asOf));
  }

  private Actor checker(String actorId, String rolesHeader, String operation) {
    if (rolesHeader == null || rolesHeader.isBlank()) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("RuleSetController")
          .operation(operation)
          .reason("X-Actor-Roles is required for a checker decision")
          .build();
    }
    Set<String> roles =
        Arrays.stream(rolesHeader.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    return new Actor(actorId, roles);
  }

  public record DecisionRequest(String remarks) {}

  public record SimulationRequest(JsonNode inputs) {}

  public record SimulationResponse(
      String ruleSetId,
      String version,
      String status,
      JsonNode evaluation,
      boolean allTestCasesPassed,
      List<TestCaseRunner.CaseResult> testCases) {}

  public record VersionPage(List<VersionResponse> items) {}

  public record Scope(String lob, String productClass) {}

  public record VersionResponse(
      String id,
      String ruleSetId,
      String version,
      String domain,
      Scope scope,
      String classification,
      String status,
      String documentSha256,
      OffsetDateTime effectiveFrom,
      OffsetDateTime effectiveTo,
      String maker,
      String checker,
      Instant approvedAt,
      String decisionRemarks,
      Instant createdAt,
      JsonNode document) {

    static VersionResponse summary(RuleSetVersion v) {
      return of(v, null);
    }

    static VersionResponse full(RuleSetVersion v) {
      try {
        return of(v, Values.mapper().readTree(v.document()));
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("stored document is not JSON", e);
      }
    }

    private static VersionResponse of(RuleSetVersion v, JsonNode document) {
      RuleSetVersion.Snapshot s = v.snapshot();
      return new VersionResponse(
          s.id(),
          s.ruleSetId(),
          s.version(),
          s.domain(),
          new Scope(s.scopeLob(), s.scopeProductClass()),
          s.classification().name(),
          s.status().name(),
          s.documentSha256(),
          s.effectiveFrom(),
          s.effectiveTo(),
          s.maker(),
          s.checker(),
          s.approvedAt(),
          s.decisionRemarks(),
          s.createdAt(),
          document);
    }
  }
}
