package com.bank.platform.integration.hub.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.integration.hub.domain.AttributionGuard;
import com.bank.platform.integration.hub.domain.CallerContexts;
import com.bank.platform.integration.hub.domain.CallerRef;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.HubAuditActions;
import com.bank.platform.integration.hub.domain.HubOutbox;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.SubmitOutcome;
import com.bank.platform.integration.hub.domain.ProviderBreakerRegistry;
import com.bank.platform.integration.hub.domain.ProviderCircuitBreaker;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import com.bank.platform.integration.hub.domain.RoutingPolicy;
import com.bank.platform.integration.hub.domain.Ulid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FUNC-032 — submit a canonical QUOTE to the routed provider adapter (once, no retry). */
@Service
public class SubmitQuoteService {

  private final ProviderRequestStore store;
  private final OnesbQuotePort onesbQuotes;
  private final HubOutbox outbox;
  private final ProviderBreakerRegistry breakers;
  private final ServiceErrors errors;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public SubmitQuoteService(
      ProviderRequestStore store,
      OnesbQuotePort onesbQuotes,
      HubOutbox outbox,
      ProviderBreakerRegistry breakers,
      ServiceErrors errors,
      Clock clock,
      ObjectMapper objectMapper) {
    this.store = Objects.requireNonNull(store);
    this.onesbQuotes = Objects.requireNonNull(onesbQuotes);
    this.outbox = Objects.requireNonNull(outbox);
    this.breakers = Objects.requireNonNull(breakers);
    this.errors = Objects.requireNonNull(errors);
    this.clock = Objects.requireNonNull(clock);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  // Persist FAILED / attribution audit then refuse the caller (same pattern as lead BLOCK).
  @Transactional(noRollbackFor = ServiceException.class)
  public ProviderRequestAccepted submit(SubmitQuoteCommand command) {
    Objects.requireNonNull(command, "command");
    if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("Idempotency-Key is required")
          .build();
    }
    if (!"QUOTE".equals(command.operation())) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("FUNC-032 supports operation QUOTE only")
          .build();
    }
    if (AttributionGuard.containsAttribution(command.rawTreeForAttribution())) {
      appendAudit(
          "attribution",
          command.idempotencyKey(),
          HubAuditActions.ATTRIBUTION_REFUSED,
          Map.of("reason", "caller supplied attribution"));
      throw errors
          .error(ErrorCodes.ATTRIBUTION_NOT_CALLER_SUPPLIED)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("attribution fields are Hub-injected")
          .build();
    }
    Optional<String> expectedContext = CallerContexts.contextForService(command.callerServiceId());
    if (expectedContext.isEmpty() || !expectedContext.get().equals(command.callerRef().context())) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("callerRef.context does not match authenticated service")
          .build();
    }

    // AT-HUB-01 / LLD §3: routing NO_ROUTE (e.g. HEALTH) → CONFIGURATION_UNRESOLVABLE.
    Optional<String> provider = RoutingPolicy.route(command.lob(), command.productClass());
    if (provider.isEmpty()) {
      throw errors
          .error(ErrorCodes.CONFIGURATION_UNRESOLVABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("HUB.ROUTING produced NO_ROUTE")
          .build();
    }
    // LLD §9 / §5.1: LIFE with unknown productClass → UNSUPPORTED_LOB (never reached for seed).
    if (!"LIFE".equalsIgnoreCase(command.lob())
        || RoutingPolicy.adapterLob(command.lob(), command.productClass()).isEmpty()) {
      throw errors
          .error(ErrorCodes.UNSUPPORTED_LOB)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("lob/productClass not supported for R0 Hub quote")
          .build();
    }
    if ("PREMIUM".equalsIgnoreCase(command.category()) && command.premiumAmount() == null) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("premiumAmount is required when category is PREMIUM")
          .build();
    }
    if ("SINGLE".equalsIgnoreCase(command.mode()) && command.selection() == null) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("selection is required when mode is SINGLE")
          .build();
    }
    assertMultiInsurerCodes(command, provider.get());

    String requestHash = hash(command);
    Optional<ProviderRequest> existing = store.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      return replay(existing.get(), requestHash);
    }

    String adapterLob =
        QuoteCanonicalMapper.requireAdapterLob(command.lob(), command.productClass()).orElseThrow();
    Instant now = clock.instant();
    ProviderRequest row =
        new ProviderRequest(
            Ulid.mint(clock),
            command.idempotencyKey(),
            requestHash,
            "QUOTE",
            command.lob().toUpperCase(Locale.ROOT),
            command.productClass().toUpperCase(Locale.ROOT),
            command.mode().toUpperCase(Locale.ROOT),
            new CallerRef(command.callerRef().context(), command.callerRef().aggregateId()),
            command.journeyId(),
            provider.get(),
            RoutingPolicy.ROUTING_VERSION,
            now);
    try {
      store.save(row);
      appendAudit(
          "provider_request",
          row.providerRequestId(),
          HubAuditActions.PROVIDER_REQUEST_DISPATCHED,
          Map.of(
              "provider", row.provider(),
              "routingVersion", row.routingVersion(),
              "configVersionRef", row.routingVersion()));
    } catch (DataAccessException ex) {
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("hub store unavailable before adapter dispatch")
          .build();
    }

    ProviderCircuitBreaker breaker = breakers.forProvider(row.provider());
    if (!breaker.allowRequest()) {
      row.markFailed(new Failure(ErrorCodes.UPSTREAM_UNAVAILABLE, "provider breaker open"), now);
      store.save(row);
      appendAudit(
          "provider_request",
          row.providerRequestId(),
          HubAuditActions.PROVIDER_REQUEST_FAILED,
          Map.of("reason", "breaker_open", "provider", row.provider()));
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("provider breaker open for " + row.provider())
          .build();
    }

    try {
      SubmitOutcome outcome =
          onesbQuotes.submitQuote(
              QuoteCanonicalMapper.toAdapterSubmit(command, adapterLob, command.actorId()));
      if (outcome instanceof SubmitOutcome.Accepted acceptedJob) {
        row.markInProgress(acceptedJob.jobId(), clock.instant());
        store.save(row);
        breaker.recordSuccess();
        return accepted(row);
      }
      row.markSubmissionUncertain(clock.instant());
      store.save(row);
      breaker.recordFailure();
      return accepted(row);
    } catch (ServiceException ex) {
      row.markFailed(
          new Failure(ex.getErrorResponse().getCode(), "adapter refused submit"), clock.instant());
      store.save(row);
      breaker.recordFailure();
      appendAudit(
          "provider_request",
          row.providerRequestId(),
          HubAuditActions.PROVIDER_REQUEST_FAILED,
          Map.of("code", ex.getErrorResponse().getCode()));
      throw ex;
    } catch (RuntimeException ex) {
      row.markFailed(
          new Failure(ErrorCodes.UPSTREAM_UNAVAILABLE, "adapter submit failed"), clock.instant());
      store.save(row);
      breaker.recordFailure();
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("1SB adapter submit failed")
          .build();
    }
  }

  private ProviderRequestAccepted replay(ProviderRequest prior, String requestHash) {
    if (!prior.requestHash().equals(requestHash)) {
      throw errors
          .error(ErrorCodes.IDEMPOTENCY_CONFLICT)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("Idempotency-Key reused with a different body")
          .build();
    }
    if (prior.state() == CanonicalState.DISPATCHING) {
      throw errors
          .error(ErrorCodes.REQUEST_IN_PROGRESS)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("provider request is still dispatching")
          .build();
    }
    if (prior.state() == CanonicalState.FAILED || prior.state() == CanonicalState.TIMED_OUT) {
      String code =
          prior.failure() == null ? ErrorCodes.UPSTREAM_BAD_RESPONSE : prior.failure().code();
      throw errors
          .error(code)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("idempotent replay of a terminal failure")
          .build();
    }
    // Prior Accepted 202 path: IN_PROGRESS / SUBMISSION_UNCERTAIN / COMPLETED / PARTIAL
    return accepted(prior);
  }

  private void assertMultiInsurerCodes(SubmitQuoteCommand command, String provider) {
    List<String> codes = command.insurerCodes();
    if (codes == null || codes.isEmpty()) {
      return;
    }
    for (String code : codes) {
      Optional<String> routed = RoutingPolicy.route(command.lob(), command.productClass());
      if (routed.isEmpty() || !provider.equals(routed.get())) {
        throw errors
            .error(ErrorCodes.CONFIGURATION_UNRESOLVABLE)
            .component("SubmitQuoteService")
            .operation("submit")
            .reason("MULTI insurerCodes do not resolve to one provider")
            .build();
      }
    }
  }

  private static ProviderRequestAccepted accepted(ProviderRequest row) {
    String state =
        row.state() == CanonicalState.SUBMISSION_UNCERTAIN
            ? CanonicalState.SUBMISSION_UNCERTAIN.name()
            : CanonicalState.IN_PROGRESS.name();
    return new ProviderRequestAccepted(
        row.providerRequestId(), row.operation(), state, row.provider(), row.routingVersion());
  }

  private void appendAudit(
      String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
    try {
      outbox.append(
          aggregateType, aggregateId, eventType, "1.0", objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException | DataAccessException ignored) {
      // Audit must not block attribution refusal / dispatch when outbox write fails after row save
      // was already attempted; DataAccessException on first insert is handled separately.
    }
  }

  private String hash(SubmitQuoteCommand command) {
    try {
      LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
      payload.put("operation", command.operation());
      payload.put("lob", command.lob());
      payload.put("productClass", command.productClass());
      payload.put("callerRef", command.callerRef());
      payload.put("journeyId", command.journeyId());
      payload.put("mode", command.mode());
      payload.put("insurerCodes", command.insurerCodes());
      payload.put("category", command.category());
      payload.put("requestedCover", command.requestedCover());
      payload.put("premiumAmount", command.premiumAmount());
      payload.put("members", command.members());
      payload.put("preferences", command.preferences());
      payload.put("selection", command.selection());
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(objectMapper.writeValueAsBytes(payload)));
    } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
      throw new IllegalStateException("unable to hash quote request", ex);
    }
  }
}
