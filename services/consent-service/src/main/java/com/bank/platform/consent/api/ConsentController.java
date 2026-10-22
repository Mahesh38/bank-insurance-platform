package com.bank.platform.consent.api;

import com.bank.common.error.ServiceErrors;
import com.bank.platform.consent.api.ConsentRequests.AnchorRetentionRequest;
import com.bank.platform.consent.api.ConsentRequests.EvaluateConsentRequest;
import com.bank.platform.consent.api.ConsentRequests.OpenGrantRequest;
import com.bank.platform.consent.api.ConsentRequests.PublishStatementRequest;
import com.bank.platform.consent.api.ConsentRequests.VerifyOtpRequest;
import com.bank.platform.consent.api.ConsentRequests.WithdrawConsentRequest;
import com.bank.platform.consent.api.ConsentViews.ChainView;
import com.bank.platform.consent.api.ConsentViews.ChallengeView;
import com.bank.platform.consent.api.ConsentViews.EvaluationView;
import com.bank.platform.consent.api.ConsentViews.EvidenceView;
import com.bank.platform.consent.api.ConsentViews.GrantView;
import com.bank.platform.consent.api.ConsentViews.RetentionView;
import com.bank.platform.consent.api.ConsentViews.StatementView;
import com.bank.platform.consent.application.ConsentApplicationService;
import com.bank.platform.consent.application.ConsentApplicationService.ChainRow;
import com.bank.platform.consent.application.ConsentApplicationService.GrantOutcome;
import com.bank.platform.consent.application.EvaluationCommand;
import com.bank.platform.consent.application.OpenGrantCommand;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentEvidence;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.EvaluationDecision;
import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.PublishedStatement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/consent")
public class ConsentController {

  static final String ACTOR = "X-Actor-Id";
  static final String IDEMPOTENCY = "Idempotency-Key";

  private final ConsentApplicationService consents;
  private final IdempotentExecutor idempotent;
  private final ServiceErrors errors;

  public ConsentController(
      ConsentApplicationService consents, IdempotentExecutor idempotent, ServiceErrors errors) {
    this.consents = consents;
    this.idempotent = idempotent;
    this.errors = errors;
  }

  @PostMapping("/statements")
  public ResponseEntity<String> publish(
      @RequestHeader(value = IDEMPOTENCY, required = false) String key,
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @Valid @RequestBody PublishStatementRequest request) {
    requireActor(actorId);
    return idempotent.execute(
        key,
        request,
        () -> {
          PublishedStatement statement =
              consents.publish(
                  request.statementId(),
                  request.version(),
                  request.text(),
                  request.language(),
                  request.changeClass(),
                  request.classifiedBy());
          return ResponseEntity.status(201).body(statementView(statement));
        });
  }

  @PostMapping("/grants")
  public ResponseEntity<String> openGrant(
      @RequestHeader(value = IDEMPOTENCY, required = false) String key,
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @Valid @RequestBody OpenGrantRequest request,
      HttpServletRequest http) {
    requireActor(actorId);
    return idempotent.execute(
        key,
        request,
        () -> {
          GrantOutcome outcome = consents.openGrant(command(request, http.getRemoteAddr()));
          int status = outcome.challengeIssued() ? 201 : 200;
          return ResponseEntity.status(status).body(grantView(outcome));
        });
  }

  @PostMapping("/challenges/{challengeId}/verify")
  public ResponseEntity<String> verify(
      @RequestHeader(value = IDEMPOTENCY, required = false) String key,
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String challengeId,
      @Valid @RequestBody VerifyOtpRequest request) {
    requireActor(actorId);
    return idempotent.execute(
        key,
        request,
        () -> {
          ConsentEvidence row = consents.verify(challengeId, request.otp(), request.actorRole());
          return ResponseEntity.status(201).body(viewOf(row));
        });
  }

  @PostMapping("/grants/{rootConsentId}/withdrawals")
  public ResponseEntity<String> withdraw(
      @RequestHeader(value = IDEMPOTENCY, required = false) String key,
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String rootConsentId,
      @Valid @RequestBody WithdrawConsentRequest request,
      HttpServletRequest http) {
    requireActor(actorId);
    return idempotent.execute(
        key,
        request,
        () -> {
          GrantOutcome outcome =
              consents.openWithdrawal(
                  rootConsentId,
                  request.registeredContactRef(),
                  request.reasonCode(),
                  request.proposalAlreadySubmitted(),
                  actorId,
                  http.getRemoteAddr());
          return ResponseEntity.status(201).body(grantView(outcome));
        });
  }

  @GetMapping("/grants/{rootConsentId}")
  public ChainView chain(
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String rootConsentId) {
    requireActor(actorId);
    List<ChainRow> rows = consents.chain(rootConsentId);
    return new ChainView(
        rootConsentId, rows.stream().map(ConsentController::evidenceView).toList());
  }

  @GetMapping("/journeys/{journeyId}")
  public List<EvidenceView> byJourney(
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String journeyId) {
    requireActor(actorId);
    return views(consents.byJourney(journeyId));
  }

  @GetMapping("/customers/{customerId}")
  public List<EvidenceView> byCustomer(
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String customerId) {
    requireActor(actorId);
    return views(consents.byCustomer(customerId));
  }

  @PostMapping("/evaluations")
  public EvaluationView evaluate(
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @Valid @RequestBody EvaluateConsentRequest request) {
    requireActor(actorId);
    EvaluationDecision decision =
        consents.evaluate(
            new EvaluationCommand(
                request.customerId(),
                request.consentCode(),
                request.counterpartyId(),
                request.currentStatementVersion(),
                request.pastBlockingPoint(),
                request.proposalAlreadySubmitted(),
                request.customerCif(),
                request.journeyId(),
                request.presentedConsentId(),
                actorId));
    if (decision.journeyBlocked()) {
      throw errors
          .error(decision.errorCode())
          .component("ConsentController")
          .operation("evaluate")
          .reason("consent evaluation refused")
          .build();
    }
    return new EvaluationView(
        decision.allowed(),
        decision.journeyBlocked(),
        decision.errorCode(),
        decision.effect().name(),
        decision.consentId(),
        decision.code().wire());
  }

  @PostMapping("/grants/{rootConsentId}/retention")
  public ResponseEntity<String> anchor(
      @RequestHeader(value = IDEMPOTENCY, required = false) String key,
      @RequestHeader(value = ACTOR, required = false) String actorId,
      @PathVariable String rootConsentId,
      @Valid @RequestBody AnchorRetentionRequest request) {
    requireActor(actorId);
    return idempotent.execute(
        key,
        request,
        () -> {
          var anchor = consents.anchorRetention(rootConsentId, request.terminalAt());
          return ResponseEntity.status(201)
              .body(
                  new RetentionView(
                      anchor.rootConsentId(),
                      anchor.terminalAt().toString(),
                      anchor.retainUntil().toString()));
        });
  }

  private List<EvidenceView> views(List<ConsentEvidence> rows) {
    Map<String, List<ChainRow>> cached = new HashMap<>();
    List<EvidenceView> out = new ArrayList<>();
    for (ConsentEvidence row : rows) {
      List<ChainRow> chain = cached.computeIfAbsent(row.rootConsentId(), consents::chain);
      ChainRow match =
          chain.stream()
              .filter(item -> item.evidence().evidenceId().equals(row.evidenceId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ConsentRefusal(ConsentCodes.RESOURCE_NOT_FOUND, "consent was not found"));
      out.add(evidenceView(match));
    }
    return out;
  }

  private EvidenceView viewOf(ConsentEvidence row) {
    return views(List.of(row)).get(0);
  }

  private static GrantView grantView(GrantOutcome outcome) {
    ChallengeView challenge =
        outcome.challenge() == null ? null : challengeView(outcome.challenge());
    EvidenceView current = null;
    if (outcome.reused() != null) {
      current = evidenceView(new ChainRow(outcome.reused(), outcome.reused().recordedState()));
    }
    return new GrantView(
        outcome.challengeIssued(),
        challenge,
        current,
        outcome.effect() == null ? null : outcome.effect().name());
  }

  private static ChallengeView challengeView(OtpChallenge challenge) {
    return new ChallengeView(
        challenge.challengeId(),
        challenge.purpose().name(),
        challenge.rootConsentId(),
        challenge.expiresAt().toString(),
        challenge.status().name());
  }

  private static StatementView statementView(PublishedStatement statement) {
    return new StatementView(
        statement.statementId(),
        statement.version(),
        statement.textHash(),
        statement.language(),
        statement.changeClass().name());
  }

  private static EvidenceView evidenceView(ChainRow row) {
    ConsentEvidence evidence = row.evidence();
    return new EvidenceView(
        evidence.evidenceId(),
        evidence.rootConsentId(),
        evidence.recordVersion(),
        evidence.supersedesEvidenceId(),
        evidence.customerId(),
        evidence.code().wire(),
        evidence.recordedState().name(),
        row.effectiveStatus().name(),
        evidence.validFrom().toString(),
        evidence.validUntil() == null ? null : evidence.validUntil().toString(),
        evidence.counterpartyId(),
        evidence.statementId(),
        evidence.statementVersion(),
        evidence.statementTextHash(),
        evidence.language(),
        evidence.leadId(),
        evidence.journeyId(),
        evidence.channel().name(),
        evidence.acknowledgementMethod(),
        evidence.withdrawalReasonCode(),
        evidence.opsTaskHint(),
        evidence.lob());
  }

  private static OpenGrantCommand command(OpenGrantRequest request, String sourceIp) {
    return new OpenGrantCommand(
        request.leadId(),
        request.journeyId(),
        request.customerId(),
        request.customerCif(),
        request.consentCode(),
        request.statementId(),
        request.statementVersion(),
        request.language(),
        request.channel(),
        request.registeredContactRef(),
        request.capturedByActorId(),
        request.capturedByAgentId(),
        request.lob(),
        request.counterpartyId(),
        request.configVersionRef(),
        sourceIp);
  }

  private static void requireActor(String actorId) {
    if (actorId == null || actorId.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "actor is required");
    }
  }
}
