package com.bank.platform.lead.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.lead.adapter.certification.FixtureSpCertification;
import com.bank.platform.lead.adapter.memory.InMemoryLeadActivityStore;
import com.bank.platform.lead.adapter.memory.InMemoryLeadExceptionEvidenceStore;
import com.bank.platform.lead.adapter.memory.InMemoryLeadIdempotencyStore;
import com.bank.platform.lead.adapter.memory.InMemoryLeadRepository;
import com.bank.platform.lead.application.LeadApplicationService.AssignLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.CreateLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.ExceptionDecisionCommand;
import com.bank.platform.lead.application.LeadApplicationService.MeetingIntent;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.ExceptionRulesPort.FiredRule;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadActivityPort.Assignment;
import com.bank.platform.lead.domain.LeadActivityPort.AssignmentKind;
import com.bank.platform.lead.domain.LeadActivityPort.FollowUp;
import com.bank.platform.lead.domain.LeadActivityPort.LeadEvent;
import com.bank.platform.lead.domain.LeadActivityPort.Meeting;
import com.bank.platform.lead.domain.LeadActivityPort.MeetingType;
import com.bank.platform.lead.domain.LeadAuthorizationPort.AuthorizationDecision;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.Decision;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.DecisionKind;
import com.bank.platform.lead.domain.LeadPolicyPort;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.RuleConfigurationException;
import com.bank.platform.lead.domain.Ulid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-030")
class LeadApplicationServiceTest {

  private static final Duration EXPIRY = Duration.ofDays(90);

  private LeadApplicationService service;
  private InMemoryLeadActivityStore activity;
  private InMemoryLeadExceptionEvidenceStore evidence;
  private AuthorizationDecision nextDecision;
  private Evaluation nextEvaluation;
  private LeadPolicyPort policy;
  private MutableClock clock;
  private final List<String> askedActions = new ArrayList<>();
  private final List<String> askedBranches = new ArrayList<>();

  @BeforeEach
  void setUp() {
    activity = new InMemoryLeadActivityStore();
    evidence = new InMemoryLeadExceptionEvidenceStore();
    nextDecision = AuthorizationDecision.allow("ROLE_GRANT");
    nextEvaluation = evaluation(ExceptionOutcome.PASS);
    policy = (lob, asOf) -> EXPIRY;
    clock = new MutableClock(Instant.now());
    service =
        new LeadApplicationService(
            new InMemoryLeadRepository(),
            ServiceErrors.of("lead", PlatformLayer.L5),
            clock,
            new FixtureSpCertification(),
            (principal, action, leadId, branchId) -> {
              askedActions.add(action);
              askedBranches.add(branchId);
              return nextDecision;
            },
            activity,
            new InMemoryLeadIdempotencyStore(),
            subject -> nextEvaluation,
            evidence,
            (lob, asOf) -> policy.expiryFor(lob, asOf));
  }

  @Test
  void createAndAssignAskThePdpThemselves() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", null, "rm-1"));

    assertThat(askedActions).containsExactly("lead.create", "lead.assign");
  }

  @Test
  void pdpDenyIsTheMappedCodeAndNothingIsWritten() {
    nextDecision = AuthorizationDecision.deny("BRANCH_OUT_OF_SCOPE");

    assertThatThrownBy(
            () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-9", "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.OUT_OF_BRANCH_SCOPE);
    assertThat(service.listVisible("rm-1")).isEmpty();
  }

  @Test
  void pdpWithoutAnswerFailsClosed() {
    nextDecision = AuthorizationDecision.unavailableDecision();

    assertThatThrownBy(
            () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.AUTHORIZATION_UNAVAILABLE);
  }

  @Test
  void sameIdempotencyKeyReplaysTheSameLead() {
    var command = new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1");
    Lead first = service.create(command, "idem-key-0001");
    Lead replay = service.create(command, "idem-key-0001");

    assertThat(replay.leadId()).isEqualTo(first.leadId());
    assertThat(activity.eventsOf(first.leadId()))
        .extracting(LeadEvent::eventType)
        .containsExactly("LeadCreated");
  }

  @Test
  void sameIdempotencyKeyWithAnotherRequestConflicts() {
    service.create(
        new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"), "idem-key-0002");

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateLeadCommand("cust-2", "LIFE", "TERM", "BR-1", "rm-1"),
                    "idem-key-0002"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void assignmentWritesHistoryMeetingNoteAndEvents() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    String tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString();
    service.assign(
        new AssignLeadCommand(
            lead.leadId(),
            "SP-1001",
            null,
            "rm-1",
            "Customer prefers a call after 6 pm",
            new MeetingIntent(MeetingType.ONLINE, tomorrow, "18:30", "https://meet.example/x")));
    service.assign(new AssignLeadCommand(lead.leadId(), "SP-1002", null, "rm-1"));

    assertThat(activity.assignmentsOf(lead.leadId()))
        .extracting(Assignment::kind)
        .containsExactly(AssignmentKind.CREATE_ASSIGN, AssignmentKind.REASSIGN);
    assertThat(activity.meetings())
        .singleElement()
        .extracting(Meeting::type)
        .isEqualTo(MeetingType.ONLINE);
    assertThat(activity.followUps())
        .singleElement()
        .extracting(FollowUp::note)
        .isEqualTo("Customer prefers a call after 6 pm");
    assertThat(activity.eventsOf(lead.leadId()))
        .extracting(LeadEvent::eventType)
        .containsExactly("LeadCreated", "LeadExceptionEvaluated", "LeadAssigned", "LeadAssigned");
  }

  @Test
  void meetingOutsideBankHoursIsRejected() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    String tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString();

    assertThatThrownBy(
            () ->
                service.assign(
                    new AssignLeadCommand(
                        lead.leadId(),
                        "SP-1001",
                        null,
                        "rm-1",
                        null,
                        new MeetingIntent(MeetingType.IN_PERSON, tomorrow, "21:15", null))))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.VALIDATION_ERROR);
  }

  @Test
  void onlineMeetingNeedsALink() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    String tomorrow = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString();

    assertThatThrownBy(
            () ->
                service.assign(
                    new AssignLeadCommand(
                        lead.leadId(),
                        "SP-1001",
                        null,
                        "rm-1",
                        null,
                        new MeetingIntent(MeetingType.ONLINE, tomorrow, "10:00", null))))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.VALIDATION_ERROR);
  }

  @Test
  void createMintsUlidAndDoesNotEvaluateException() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThat(Ulid.isValid(lead.leadId())).isTrue();
    assertThat(lead.state()).isEqualTo(LeadState.NEW);
    assertThat(lead.exceptionOutcome()).isEqualTo(ExceptionOutcome.NOT_EVALUATED);
    assertThat(lead.journeyId()).isEqualTo(lead.leadId());
  }

  @Test
  void createConflictsOnUnfinishedDedupeKey() {
    service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThatThrownBy(
            () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.CONFLICT);
  }

  @Test
  void assignRequiresOnboardingAndCertifiedSp() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThatThrownBy(
            () -> service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", null, "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);

    service.startOnboarding(lead.leadId(), "rm-1");
    Lead assigned =
        service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", "SP-1001", "rm-1"));
    assertThat(assigned.state()).isEqualTo(LeadState.ASSIGNED);
    assertThat(assigned.accountableSpId()).isEqualTo("SP-1001");
    assertThat(assigned.exceptionOutcome()).isEqualTo(ExceptionOutcome.PASS);
  }

  @Test
  void startOnboardingBlocksWhenTheRulesBlock() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    nextEvaluation = evaluation(ExceptionOutcome.BLOCK);

    assertThatThrownBy(() -> service.startOnboarding(lead.leadId(), "rm-1"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void uncertifiedAssigneeIsRejected() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");

    assertThatThrownBy(
            () -> service.assign(new AssignLeadCommand(lead.leadId(), "UNCERTIFIED", null, "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.SP_CERTIFICATION_REQUIRED);

    assertThatThrownBy(
            () -> service.assign(new AssignLeadCommand(lead.leadId(), "sp-1", null, "rm-1")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.SP_CERTIFICATION_REQUIRED);
  }

  @Test
  void foreignBookIsAbsentNotForbidden() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThatThrownBy(() -> service.getVisible(lead.leadId(), "other-rm"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
  }

  @Test
  void dedupeConflictCarriesTheExistingLeadId() {
    Lead existing = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThatThrownBy(
            () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              var response = ((ServiceException) ex).getErrorResponse();
              assertThat(response.getCode()).isEqualTo(ErrorCodes.CONFLICT);
              assertThat(response.getErrors())
                  .singleElement()
                  .satisfies(
                      e -> {
                        assertThat(e.field()).isEqualTo("leadId");
                        assertThat(e.message()).isEqualTo(existing.leadId());
                      });
            });
  }

  @Test
  void closedLeadCannotBeAssignedClosedAgainOrReEvaluated() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    service.close(lead.leadId(), "rm-1");

    assertCode(
        () -> service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", null, "rm-1")),
        ErrorCodes.ILLEGAL_TRANSITION);
    assertCode(() -> service.close(lead.leadId(), "rm-1"), ErrorCodes.ILLEGAL_TRANSITION);
    assertCode(() -> service.startOnboarding(lead.leadId(), "rm-1"), ErrorCodes.ILLEGAL_TRANSITION);
    assertThat(service.getVisible(lead.leadId(), "rm-1").state()).isEqualTo(LeadState.DISQUALIFIED);
  }

  @Test
  void reEvaluationNeverClearsAnApprovalHold() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    nextEvaluation = evaluation(ExceptionOutcome.APPROVAL_REQUIRED);
    Lead held = service.startOnboarding(lead.leadId(), "rm-1");
    assertThat(held.exceptionHold()).isTrue();

    nextEvaluation = evaluation(ExceptionOutcome.PASS);
    assertThatThrownBy(() -> service.startOnboarding(lead.leadId(), "rm-1"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              var response = ((ServiceException) ex).getErrorResponse();
              assertThat(response.getCode()).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
              assertThat(((ServiceException) ex).getDiagnostic().getReason())
                  .contains("exception hold awaiting approval");
            });

    Lead after = service.getVisible(lead.leadId(), "rm-1");
    assertThat(after.exceptionHold()).isTrue();
    assertThat(after.exceptionOutcome()).isEqualTo(ExceptionOutcome.APPROVAL_REQUIRED);
  }

  @Test
  void blockStaysBlockOnReEvaluation() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    nextEvaluation = evaluation(ExceptionOutcome.BLOCK);
    assertCode(() -> service.startOnboarding(lead.leadId(), "rm-1"), ErrorCodes.ILLEGAL_TRANSITION);

    nextEvaluation = evaluation(ExceptionOutcome.PASS);
    assertCode(() -> service.startOnboarding(lead.leadId(), "rm-1"), ErrorCodes.ILLEGAL_TRANSITION);
    assertThat(service.getVisible(lead.leadId(), "rm-1").exceptionOutcome())
        .isEqualTo(ExceptionOutcome.BLOCK);
  }

  @Test
  void expiryComesFromLeadPolicy() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

    assertThat(lead.expiresAt()).isEqualTo(lead.createdAt().plus(90, ChronoUnit.DAYS));
  }

  @Test
  void createIsRefusedWhenLeadPolicyIsUnusable() {
    policy =
        (lob, asOf) -> {
          throw new RuleConfigurationException("no LEAD.POLICY in force", false, null);
        };
    assertCode(
        () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")),
        ErrorCodes.CONFIGURATION_UNRESOLVABLE);

    policy =
        (lob, asOf) -> {
          throw new RuleConfigurationException("config service down", true, null);
        };
    assertCode(
        () -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")),
        ErrorCodes.UPSTREAM_UNAVAILABLE);
    assertThat(service.listVisible("rm-1")).isEmpty();
  }

  @Test
  void everyEvaluationIsStoredAsEvidence() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    nextEvaluation = held("BANK_ADMIN");
    Lead held = service.startOnboarding(lead.leadId(), "rm-1");

    assertThat(evidence.evaluations())
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.evaluationId()).isEqualTo(held.exceptionEvaluationId());
              assertThat(e.evaluation().ruleSetRef()).isEqualTo("LEAD.EXCEPTION@0.0.1");
              assertThat(e.evaluatedBy()).isEqualTo("rm-1");
            });
    assertThat(activity.eventsOf(lead.leadId()).get(1).attributes())
        .containsEntry("evaluationId", held.exceptionEvaluationId())
        .containsEntry("ruleSetRef", "LEAD.EXCEPTION@0.0.1");
  }

  @Test
  void bankApproverReleasesTheHoldAndAssignmentProceeds() {
    Lead lead = heldLead("BANK_ADMIN");

    Lead released =
        service.decideException(decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN"));

    assertThat(released.exceptionHold()).isFalse();
    assertThat(released.exceptionOutcome()).isEqualTo(ExceptionOutcome.APPROVAL_REQUIRED);
    assertThat(askedActions).contains("lead.exception.approve");
    assertThat(askedBranches.get(askedActions.indexOf("lead.exception.approve")))
        .isEqualTo("BR-1");
    assertThat(evidence.decisionsOf(lead.leadId()))
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.decision()).isEqualTo(DecisionKind.APPROVED);
              assertThat(d.remarks()).isEqualTo("age exception approved by branch");
              assertThat(d.decidedByRole()).isEqualTo("BANK_ADMIN");
              assertThat(d.evaluationId()).isEqualTo(lead.exceptionEvaluationId());
            });
    assertThat(activity.eventsOf(lead.leadId()))
        .extracting(LeadEvent::eventType)
        .contains("LeadExceptionApproved");

    Lead assigned =
        service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", null, "rm-1"));
    assertThat(assigned.state()).isEqualTo(LeadState.ASSIGNED);
    // An approved exception is not re-evaluated (it could only overwrite the decision).
    assertCode(() -> service.startOnboarding(lead.leadId(), "rm-1"), ErrorCodes.ILLEGAL_TRANSITION);
    // A second decision on the same hold is refused.
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.REJECTED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void rejectionClosesTheLeadAndAllowsANewOne() {
    Lead lead = heldLead("BANK_ADMIN");

    Lead rejected =
        service.decideException(decision(lead, DecisionKind.REJECTED, "BANK_EMPLOYEE", "BANK_ADMIN"));

    assertThat(rejected.state()).isEqualTo(LeadState.DISQUALIFIED);
    assertThat(rejected.exceptionOutcome()).isEqualTo(ExceptionOutcome.BLOCK);
    assertThat(rejected.exceptionHold()).isFalse();
    assertThat(activity.eventsOf(lead.leadId()))
        .extracting(LeadEvent::eventType)
        .contains("LeadExceptionRejected");
    assertThat(evidence.decisionsOf(lead.leadId()))
        .extracting(Decision::decision)
        .containsExactly(DecisionKind.REJECTED);
    assertCode(
        () -> service.assign(new AssignLeadCommand(lead.leadId(), "SP-1001", null, "rm-1")),
        ErrorCodes.ILLEGAL_TRANSITION);
    // BRD §8.8: a new journey may be created for the customer.
    assertThat(service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
        .isNotNull();
  }

  @Test
  void insurerPartnerNeverDecidesEvenWhenThePdpAllows() {
    Lead lead = heldLead("BANK_ADMIN");

    for (String type : List.of("INSURER_PARTNER_REP", "insurer_representative")) {
      assertCode(
          () ->
              service.decideException(
                  decision(lead, DecisionKind.APPROVED, type, "BANK_ADMIN")),
          ErrorCodes.FORBIDDEN);
    }
    assertCode(
        () -> service.decideException(decision(lead, DecisionKind.APPROVED, null, "BANK_ADMIN")),
        ErrorCodes.FORBIDDEN);
    assertThat(askedActions).doesNotContain("lead.exception.approve");
    assertThat(service.getVisible(lead.leadId(), "rm-1").exceptionHold()).isTrue();
  }

  @Test
  void approverRoleMustBeConfiguredForEveryTriggeredRule() {
    Lead lead = heldLead("BANK_ADMIN");
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BRANCH_MANAGER")),
        ErrorCodes.FORBIDDEN);

    // Two rules ask for approval: only a role both allow may decide (EH-BR-MULTI-002, strict).
    nextEvaluation =
        new Evaluation(
            ExceptionOutcome.APPROVAL_REQUIRED,
            List.of("EH-ERR-RULE-001", "EH-ERR-RULE-003"),
            "LEAD.EXCEPTION",
            "0.0.1",
            "1.0.0",
            "sha256:x",
            List.of(
                new FiredRule(
                    "EH-BR-RULE-001",
                    "APPROVAL_REQUIRED",
                    "EH-ERR-RULE-001",
                    List.of("BANK_ADMIN", "BRANCH_MANAGER"),
                    List.of()),
                new FiredRule(
                    "EH-BR-RULE-003",
                    "APPROVAL_REQUIRED",
                    "EH-ERR-RULE-003",
                    List.of("BANK_ADMIN"),
                    List.of())),
            List.of(),
            "{}");
    Lead two = service.create(new CreateLeadCommand("cust-2", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(two.leadId(), "rm-1");
    assertCode(
        () ->
            service.decideException(
                decision(two, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BRANCH_MANAGER")),
        ErrorCodes.FORBIDDEN);
    assertThat(
            service
                .decideException(decision(two, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN"))
                .exceptionHold())
        .isFalse();
  }

  @Test
  void noConfiguredApproverMeansNobodyDecides() {
    Lead lead = heldLead();
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.FORBIDDEN);
  }

  @Test
  void pdpDenyAndMissingRemarksAreRefused() {
    Lead lead = heldLead("BANK_ADMIN");
    assertCode(
        () ->
            service.decideException(
                new ExceptionDecisionCommand(
                    lead.leadId(),
                    DecisionKind.APPROVED,
                    "approver-1",
                    "BANK_EMPLOYEE",
                    Set.of("BANK_ADMIN"),
                    " ",
                    null)),
        ErrorCodes.VALIDATION_ERROR);
    assertCode(
        () ->
            service.decideException(
                new ExceptionDecisionCommand(
                    lead.leadId(),
                    DecisionKind.APPROVED,
                    "approver-1",
                    "BANK_EMPLOYEE",
                    Set.of("BANK_ADMIN"),
                    "x".repeat(501),
                    null)),
        ErrorCodes.VALIDATION_ERROR);

    nextDecision = AuthorizationDecision.deny("PARTNER_ACTION_PROHIBITED");
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.FORBIDDEN);
    nextDecision = AuthorizationDecision.deny("BRANCH_OUT_OF_SCOPE");
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.OUT_OF_BRANCH_SCOPE);
    nextDecision = AuthorizationDecision.unavailableDecision();
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.AUTHORIZATION_UNAVAILABLE);
    assertThat(evidence.decisionsOf(lead.leadId())).isEmpty();
  }

  @Test
  void aLeadWithoutHoldCannotBeDecided() {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    service.startOnboarding(lead.leadId(), "rm-1");
    assertCode(
        () ->
            service.decideException(
                decision(lead, DecisionKind.APPROVED, "BANK_EMPLOYEE", "BANK_ADMIN")),
        ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void overdueUnfinishedLeadsExpireOnceAndOthersStay() {
    Lead open = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    Lead closed = service.create(new CreateLeadCommand("cust-2", "LIFE", "TERM", "BR-1", "rm-1"));
    service.close(closed.leadId(), "rm-1");

    assertThat(service.expireOverdue(10)).isZero();
    clock.advance(EXPIRY.plusSeconds(1));
    assertThat(service.expireOverdue(10)).isEqualTo(1);
    assertThat(service.expireOverdue(10)).isZero();

    assertThat(service.getVisible(open.leadId(), "rm-1").state()).isEqualTo(LeadState.EXPIRED);
    assertThat(service.getVisible(closed.leadId(), "rm-1").state())
        .isEqualTo(LeadState.DISQUALIFIED);
    assertThat(activity.eventsOf(open.leadId()))
        .extracting(LeadEvent::eventType)
        .containsExactly("LeadCreated", "LeadExpired");
    // An expired lead no longer blocks a fresh one for the same customer (dedupe).
    assertThat(service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
        .isNotNull();
  }

  private Lead heldLead(String... roles) {
    Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    nextEvaluation = held(roles);
    return service.startOnboarding(lead.leadId(), "rm-1");
  }

  private static Evaluation held(String... roles) {
    return new Evaluation(
        ExceptionOutcome.APPROVAL_REQUIRED,
        List.of("EH-ERR-RULE-001"),
        "LEAD.EXCEPTION",
        "0.0.1",
        "1.0.0",
        "sha256:x",
        List.of(
            new FiredRule(
                "EH-BR-RULE-001",
                "APPROVAL_REQUIRED",
                "EH-ERR-RULE-001",
                List.of(roles),
                List.of())),
        List.of("The Proposer's age exceeds the maximum limit of 60 years."),
        "{}");
  }

  private static ExceptionDecisionCommand decision(
      Lead lead, DecisionKind kind, String actorType, String role) {
    return new ExceptionDecisionCommand(
        lead.leadId(),
        kind,
        "approver-1",
        actorType,
        Set.of(role),
        "age exception approved by branch",
        "AUBIMA-42");
  }

  /** A clock tests can move forward. */
  static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static Evaluation evaluation(ExceptionOutcome outcome) {
    return new Evaluation(outcome, List.of(), null);
  }

  private static void assertCode(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(code);
  }
}
