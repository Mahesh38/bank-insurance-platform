package com.bank.platform.lead.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.lead.adapter.certification.FixtureSpCertification;
import com.bank.platform.lead.adapter.memory.InMemoryLeadActivityStore;
import com.bank.platform.lead.adapter.memory.InMemoryLeadIdempotencyStore;
import com.bank.platform.lead.adapter.memory.InMemoryLeadRepository;
import com.bank.platform.lead.application.LeadApplicationService.AssignLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.CreateLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.MeetingIntent;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadActivityPort.Assignment;
import com.bank.platform.lead.domain.LeadActivityPort.AssignmentKind;
import com.bank.platform.lead.domain.LeadActivityPort.FollowUp;
import com.bank.platform.lead.domain.LeadActivityPort.LeadEvent;
import com.bank.platform.lead.domain.LeadActivityPort.Meeting;
import com.bank.platform.lead.domain.LeadActivityPort.MeetingType;
import com.bank.platform.lead.domain.LeadAuthorizationPort.AuthorizationDecision;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.Ulid;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-030")
class LeadApplicationServiceTest {

  private LeadApplicationService service;
  private InMemoryLeadActivityStore activity;
  private AuthorizationDecision nextDecision;
  private Evaluation nextEvaluation;
  private final List<String> askedActions = new ArrayList<>();

  @BeforeEach
  void setUp() {
    activity = new InMemoryLeadActivityStore();
    nextDecision = AuthorizationDecision.allow("ROLE_GRANT");
    nextEvaluation = evaluation(ExceptionOutcome.PASS);
    service =
        new LeadApplicationService(
            new InMemoryLeadRepository(),
            ServiceErrors.of("lead", PlatformLayer.L5),
            Clock.systemUTC(),
            new FixtureSpCertification(),
            (principal, action, leadId, branchId) -> {
              askedActions.add(action);
              return nextDecision;
            },
            activity,
            new InMemoryLeadIdempotencyStore(),
            subject -> nextEvaluation,
            LeadSettings.defaults());
  }

  private LeadApplicationService serviceWith(LeadSettings settings) {
    return new LeadApplicationService(
        new InMemoryLeadRepository(),
        ServiceErrors.of("lead", PlatformLayer.L5),
        Clock.systemUTC(),
        new FixtureSpCertification(),
        (principal, action, leadId, branchId) -> AuthorizationDecision.allow("ROLE_GRANT"),
        new InMemoryLeadActivityStore(),
        new InMemoryLeadIdempotencyStore(),
        subject -> nextEvaluation,
        settings);
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
  void expiryIsSetOnlyWhenConfigured() {
    Lead unset = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    assertThat(unset.expiresAt()).isNull();

    Lead set =
        serviceWith(new LeadSettings(Duration.ofDays(30)))
            .create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
    assertThat(set.expiresAt()).isEqualTo(set.createdAt().plus(30, ChronoUnit.DAYS));
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
