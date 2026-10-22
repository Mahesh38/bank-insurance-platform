package com.bank.platform.lead.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.IllegalLeadTransitionException;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadActivityPort;
import com.bank.platform.lead.domain.LeadActivityPort.Assignment;
import com.bank.platform.lead.domain.LeadActivityPort.AssignmentKind;
import com.bank.platform.lead.domain.LeadActivityPort.FollowUp;
import com.bank.platform.lead.domain.LeadActivityPort.LeadEvent;
import com.bank.platform.lead.domain.LeadActivityPort.Meeting;
import com.bank.platform.lead.domain.LeadActivityPort.MeetingType;
import com.bank.platform.lead.domain.LeadAuthorizationPort;
import com.bank.platform.lead.domain.LeadAuthorizationPort.AuthorizationDecision;
import com.bank.platform.lead.domain.LeadIdempotencyPort;
import com.bank.platform.lead.domain.LeadIdempotencyPort.IdempotencyRecord;
import com.bank.platform.lead.domain.LeadRepository;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.ProductClass;
import com.bank.platform.lead.domain.SpCertificationPort;
import com.bank.platform.lead.domain.Ulid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lead commands for the R0 assisted slice ({@code D-019}, {@code D-020}, {@code D-021}).
 *
 * <p>Persistence is {@code lead_lms} through {@link LeadRepository} and {@link LeadActivityPort}
 * (JDBC adapters, Flyway V2–V3). Each command is one local transaction: the lead row, its history /
 * meeting / note rows and its outbox event commit together; a concurrent change to the same lead is
 * a {@code 409 CONFLICT}, not a lost update.
 *
 * <p>Create and assign ask the PDP again ({@code INV-ACT-01}, {@code SUG-20261021-lft}); the BFF's
 * allow is not trusted across the hop. No answer is a deny.
 */
@Service
public class LeadApplicationService {

  static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
  // Lead BRD Screen 7: a meeting may be booked between 08:00 and 20:00 bank time.
  private static final ZoneId BANK_ZONE = ZoneId.of("Asia/Kolkata");
  private static final LocalTime MEETING_FROM = LocalTime.of(8, 0);
  private static final LocalTime MEETING_UNTIL = LocalTime.of(20, 0);
  private static final int NOTE_MAX = 250;

  private final LeadRepository leads;
  private final ServiceErrors errors;
  private final Clock clock;
  private final SpCertificationPort certification;
  private final LeadAuthorizationPort authorization;
  private final LeadActivityPort activity;
  private final LeadIdempotencyPort idempotency;
  private final ExceptionRulesPort exceptionRules;
  private final LeadSettings settings;

  public LeadApplicationService(
      LeadRepository leads,
      ServiceErrors errors,
      Clock clock,
      SpCertificationPort certification,
      LeadAuthorizationPort authorization,
      LeadActivityPort activity,
      LeadIdempotencyPort idempotency,
      ExceptionRulesPort exceptionRules,
      LeadSettings settings) {
    this.leads = leads;
    this.errors = errors;
    this.clock = clock;
    this.certification = certification;
    this.authorization = authorization;
    this.activity = activity;
    this.idempotency = idempotency;
    this.exceptionRules = exceptionRules;
    this.settings = settings;
  }

  @Transactional
  public Lead create(CreateLeadCommand command) {
    return create(command, null);
  }

  /**
   * {@code idempotencyKey} is honoured when present: same key and same request returns the lead the
   * first call created; same key with a different request is {@code 409 IDEMPOTENCY_CONFLICT}. The
   * contract makes the header mandatory; it is optional here until NIP-APP sends it.
   */
  @Transactional
  public Lead create(CreateLeadCommand command, String idempotencyKey) {
    require(command.customerId(), "customerId");
    require(command.createdByPrincipalId(), "createdByPrincipalId");
    authorize(command.createdByPrincipalId(), "lead.create", null, command.branchId(), "create");
    Instant requestedAt = clock.instant();
    String requestHash = requestHash(command);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      Optional<Lead> replay = replay(idempotencyKey, requestHash, requestedAt);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!"LIFE".equals(command.lob())) {
      throw errors
          .error(ErrorCodes.UNSUPPORTED_LOB)
          .component("LeadApplicationService")
          .operation("create")
          .reason("lob must be LIFE")
          .build();
    }
    ProductClass productClass = parseProductClass(command.productClass());
    leads
        .findUnfinished(command.createdByPrincipalId(), command.customerId(), productClass)
        .ifPresent(
            existing -> {
              // errors[0].message is the existing ULID only, so the app can offer "Continue
              // existing" (Lead BRD Table 18) without parsing prose.
              throw errors
                  .error(ErrorCodes.CONFLICT)
                  .component("LeadApplicationService")
                  .operation("create")
                  .reason("unfinished lead exists for this creator, customer and product class")
                  .errors(
                      List.of(
                          ServiceError.ofField(ErrorCodes.CONFLICT, existing.leadId(), "leadId")))
                  .build();
            });
    Instant now = clock.instant();
    Lead lead =
        new Lead(
            Ulid.mint(clock),
            command.customerId(),
            command.lob(),
            productClass,
            command.branchId(),
            command.createdByPrincipalId(),
            now);
    if (settings.expiry() != null) {
      lead.expireAt(now.plus(settings.expiry()));
    }
    persist(lead);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      try {
        idempotency.save(
            new IdempotencyRecord(
                idempotencyKey, requestHash, lead.leadId(), now, now.plus(IDEMPOTENCY_TTL)));
      } catch (ConcurrentLeadUpdateException ex) {
        throw errors
            .error(ErrorCodes.CONFLICT)
            .component("LeadApplicationService")
            .operation("create")
            .reason("the same Idempotency-Key is being processed by another request")
            .cause(ex)
            .build();
      }
    }
    event(lead, "LeadCreated", now);
    return lead;
  }

  public Lead getVisible(String leadId, String principalId) {
    Lead lead = leads.findById(leadId).orElseThrow(() -> notFound());
    if (!lead.visibleTo(principalId)) {
      throw notFound();
    }
    return lead;
  }

  public List<Lead> listVisible(String principalId) {
    return listVisible(principalId, null, null, false);
  }

  public List<Lead> listVisible(
      String principalId, String customerId, String productClass, boolean unfinishedOnly) {
    return leads.findVisibleTo(principalId).stream()
        .filter(lead -> customerId == null || customerId.equals(lead.customerId()))
        .filter(lead -> productClass == null || productClass.equals(lead.productClass().name()))
        .filter(lead -> !unfinishedOnly || lead.unfinished())
        .toList();
  }

  /** Start Onboarding evaluates exception rules. Save/create never does ({@code D-019}). */
  @Transactional(noRollbackFor = ServiceException.class) // a BLOCK outcome is saved, then refused
  public Lead startOnboarding(String leadId, String principalId) {
    Lead lead = getVisible(leadId, principalId);
    // Guards first: a held, blocked or terminal lead is refused before any rule is evaluated.
    Instant now = clock.instant();
    transition("startOnboarding", lead::requireEvaluable);
    Evaluation evaluation =
        exceptionRules.evaluate(
            new ExceptionRulesPort.Subject(
                lead.leadId(), lead.customerId(), lead.lob(), lead.productClass()));
    ExceptionOutcome outcome = evaluation.outcome();
    transition("startOnboarding", () -> lead.applyExceptionOutcome(outcome, now));
    persist(lead);
    event(lead, "LeadExceptionEvaluated", now, evaluation.reasonCodes());
    if (outcome == ExceptionOutcome.BLOCK) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("startOnboarding")
          .reason("exception rules blocked onboarding")
          .build();
    }
    return lead;
  }

  @Transactional
  public Lead assign(AssignLeadCommand command) {
    Lead lead = getVisible(command.leadId(), command.actorPrincipalId());
    authorize(command.actorPrincipalId(), "lead.assign", lead.leadId(), lead.branchId(), "assign");
    if (lead.terminal()) {
      throw illegal("assign", "cannot assign a lead in state " + lead.state());
    }
    if (lead.biGenerated()) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("reassignment after BI is not permitted")
          .build();
    }
    if (lead.exceptionOutcome() == ExceptionOutcome.NOT_EVALUATED) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("start onboarding before assignment")
          .build();
    }
    if (lead.exceptionOutcome() == ExceptionOutcome.BLOCK || lead.exceptionHold()) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("exception hold is active")
          .build();
    }
    require(command.assignedRmId(), "assignedRmId");
    if (!certification.holdsValidCertification(command.assignedRmId())) {
      throw errors
          .error(ErrorCodes.SP_CERTIFICATION_REQUIRED)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("assignee must hold a valid SP certification")
          .build();
    }
    validateNote(command.note());
    Instant now = clock.instant();
    Instant meetingAt = command.meeting() == null ? null : meetingTime(command.meeting(), now);
    AssignmentKind kind =
        lead.assignedRmId() == null ? AssignmentKind.CREATE_ASSIGN : AssignmentKind.REASSIGN;
    transition("assign", () -> lead.assign(command.assignedRmId(), command.assignedSpId(), now));
    persist(lead);
    activity.recordAssignment(
        new Assignment(
            Ulid.mint(clock),
            lead.leadId(),
            command.assignedRmId(),
            command.assignedSpId(),
            now,
            command.actorPrincipalId(),
            kind));
    if (command.meeting() != null) {
      activity.recordMeeting(
          new Meeting(
              Ulid.mint(clock),
              lead.leadId(),
              command.meeting().type(),
              meetingAt,
              command.meeting().link(),
              command.actorPrincipalId(),
              now));
    }
    if (command.note() != null && !command.note().isBlank()) {
      activity.recordFollowUp(
          new FollowUp(
              Ulid.mint(clock), lead.leadId(), command.note(), command.actorPrincipalId(), now));
    }
    event(lead, "LeadAssigned", now);
    return lead;
  }

  @Transactional
  public Lead close(String leadId, String principalId) {
    Lead lead = getVisible(leadId, principalId);
    if (lead.terminal()) {
      throw illegal("close", "cannot close a lead in state " + lead.state());
    }
    if (lead.state() == LeadState.QUALIFIED || lead.biGenerated()) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("close")
          .reason("eligible leads are not closed from the diary path")
          .build();
    }
    Instant now = clock.instant();
    transition("close", () -> lead.close(now));
    persist(lead);
    event(lead, "LeadClosed", now);
    return lead;
  }

  private ProductClass parseProductClass(String raw) {
    try {
      return ProductClass.valueOf(raw);
    } catch (RuntimeException ex) {
      throw errors
          .error(ErrorCodes.UNSUPPORTED_LOB)
          .component("LeadApplicationService")
          .operation("create")
          .reason("product class is not TERM, SAVINGS or ULIP")
          .cause(ex)
          .build();
    }
  }

  private void authorize(
      String principalId, String action, String leadId, String branchId, String operation) {
    AuthorizationDecision decision = authorization.decide(principalId, action, leadId, branchId);
    if (decision.allowed()) {
      return;
    }
    String code =
        decision.unavailable()
            ? ErrorCodes.AUTHORIZATION_UNAVAILABLE
            : errorCodeFor(decision.reasonCode());
    throw errors
        .error(code)
        .component("LeadApplicationService")
        .operation(operation)
        .reason(action + " refused by the PDP: " + decision.reasonCode())
        .build();
  }

  /** Same mapping as the BFF's {@code PolicyDecisionClient}, so the app sees one vocabulary. */
  static String errorCodeFor(String reasonCode) {
    if (reasonCode == null) {
      return ErrorCodes.DEFAULT_DENY;
    }
    return switch (reasonCode) {
      case "ACCOUNT_NOT_ACTIVE" -> ErrorCodes.ACCOUNT_SUSPENDED;
      case "CROSS_INSURER_ACCESS" -> ErrorCodes.CROSS_INSURER_DENIED;
      case "EXPLICIT_DENY" -> ErrorCodes.EXPLICIT_DENY;
      case "BRANCH_OUT_OF_SCOPE" -> ErrorCodes.OUT_OF_BRANCH_SCOPE;
      case "RESOURCE_NOT_SHARED_WITH_PARTNER" -> ErrorCodes.RESOURCE_NOT_FOUND;
      case "MANDATORY_CERTIFICATION_INVALID" -> ErrorCodes.SP_CERTIFICATION_REQUIRED;
      default -> ErrorCodes.DEFAULT_DENY;
    };
  }

  private Optional<Lead> replay(String key, String requestHash, Instant now) {
    Optional<IdempotencyRecord> previous = idempotency.find(key, now);
    if (previous.isEmpty()) {
      return Optional.empty();
    }
    if (!previous.get().requestHash().equals(requestHash)) {
      throw errors
          .error(ErrorCodes.IDEMPOTENCY_CONFLICT)
          .component("LeadApplicationService")
          .operation("create")
          .reason("Idempotency-Key was used with a different request")
          .build();
    }
    return leads.findById(previous.get().responseRef());
  }

  // The actor is part of the hash: another RM replaying someone else's key is a conflict.
  private static String requestHash(CreateLeadCommand command) {
    String canonical =
        String.join(
            "|",
            String.valueOf(command.customerId()),
            String.valueOf(command.lob()),
            String.valueOf(command.productClass()),
            String.valueOf(command.branchId()),
            String.valueOf(command.createdByPrincipalId()));
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is required by the JDK", ex);
    }
  }

  private void event(Lead lead, String type, Instant at) {
    event(lead, type, at, List.of());
  }

  private void event(Lead lead, String type, Instant at, List<String> reasonCodes) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("state", lead.state().name());
    attributes.put("productClass", lead.productClass().name());
    attributes.put("exceptionOutcome", lead.exceptionOutcome().name());
    if (lead.assignedRmId() != null) {
      attributes.put("assignedRmId", lead.assignedRmId());
    }
    if (lead.journeyId() != null) {
      attributes.put("journeyId", lead.journeyId());
    }
    if (!reasonCodes.isEmpty()) {
      attributes.put("exceptionReasonCodes", String.join(",", reasonCodes));
    }
    activity.append(new LeadEvent(Ulid.mint(clock), lead.leadId(), type, attributes, at));
  }

  private void validateNote(String note) {
    if (note != null && note.length() > NOTE_MAX) {
      throw invalid("note is longer than " + NOTE_MAX + " characters");
    }
  }

  private Instant meetingTime(MeetingIntent meeting, Instant now) {
    if (meeting.type() == null || meeting.date() == null || meeting.time() == null) {
      throw invalid("meeting needs type, date and time");
    }
    if (meeting.type() == MeetingType.ONLINE
        && (meeting.link() == null || meeting.link().isBlank())) {
      throw invalid("an online meeting needs a link");
    }
    if (meeting.type() == MeetingType.IN_PERSON && meeting.link() != null) {
      throw invalid("an in-person meeting has no link");
    }
    LocalDate date;
    LocalTime time;
    try {
      date = LocalDate.parse(meeting.date());
      time = LocalTime.parse(meeting.time());
    } catch (DateTimeParseException ex) {
      throw invalid("meeting date must be yyyy-MM-dd and time HH:mm");
    }
    if (time.isBefore(MEETING_FROM) || time.isAfter(MEETING_UNTIL)) {
      throw invalid("meeting time must be between 08:00 and 20:00");
    }
    Instant at = date.atTime(time).atZone(BANK_ZONE).toInstant();
    if (at.isBefore(now)) {
      throw invalid("meeting cannot be in the past");
    }
    return at;
  }

  /** Runs a domain transition; a refusal by the aggregate is {@code 409 ILLEGAL_TRANSITION}. */
  private void transition(String operation, Runnable change) {
    try {
      change.run();
    } catch (IllegalLeadTransitionException ex) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation(operation)
          .reason(ex.getMessage())
          .cause(ex)
          .build();
    }
  }

  private ServiceException illegal(String operation, String reason) {
    return errors
        .error(ErrorCodes.ILLEGAL_TRANSITION)
        .component("LeadApplicationService")
        .operation(operation)
        .reason(reason)
        .build();
  }

  private ServiceException invalid(String reason) {
    return errors
        .error(ErrorCodes.VALIDATION_ERROR)
        .component("LeadApplicationService")
        .operation("assign")
        .reason(reason)
        .build();
  }

  private void persist(Lead lead) {
    try {
      leads.save(lead);
    } catch (ConcurrentLeadUpdateException ex) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("LeadApplicationService")
          .operation("persist")
          .reason("lead was changed by another request; re-read and retry")
          .cause(ex)
          .build();
    }
  }

  private void require(String value, String field) {
    if (value == null || value.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_REQUIRED_FIELD)
          .component("LeadApplicationService")
          .operation("validate")
          .reason(field + " is required")
          .build();
    }
  }

  private RuntimeException notFound() {
    return errors
        .error(ErrorCodes.RESOURCE_NOT_FOUND)
        .component("LeadApplicationService")
        .operation("get")
        .reason("lead is absent from this book")
        .build();
  }

  public record CreateLeadCommand(
      String customerId,
      String lob,
      String productClass,
      String branchId,
      String createdByPrincipalId) {}

  public record AssignLeadCommand(
      String leadId,
      String assignedRmId,
      String assignedSpId,
      String actorPrincipalId,
      String note,
      MeetingIntent meeting) {

    public AssignLeadCommand(
        String leadId, String assignedRmId, String assignedSpId, String actorPrincipalId) {
      this(leadId, assignedRmId, assignedSpId, actorPrincipalId, null, null);
    }
  }

  /** Lead BRD Screen 7 meeting capture: local date and time in bank time. */
  public record MeetingIntent(MeetingType type, String date, String time, String link) {}
}
