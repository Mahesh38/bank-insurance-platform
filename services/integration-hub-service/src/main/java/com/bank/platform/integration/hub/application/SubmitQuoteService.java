package com.bank.platform.integration.hub.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.integration.hub.domain.AttributionGuard;
import com.bank.platform.integration.hub.domain.CallerContexts;
import com.bank.platform.integration.hub.domain.CallerRef;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.SubmitOutcome;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import com.bank.platform.integration.hub.domain.RoutingPolicy;
import com.bank.platform.integration.hub.domain.Ulid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** FUNC-032 — submit a canonical QUOTE to the routed provider adapter (once, no retry). */
@Service
public class SubmitQuoteService {

  private final ProviderRequestStore store;
  private final OnesbQuotePort onesbQuotes;
  private final ServiceErrors errors;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public SubmitQuoteService(
      ProviderRequestStore store,
      OnesbQuotePort onesbQuotes,
      ServiceErrors errors,
      Clock clock,
      ObjectMapper objectMapper) {
    this.store = Objects.requireNonNull(store);
    this.onesbQuotes = Objects.requireNonNull(onesbQuotes);
    this.errors = Objects.requireNonNull(errors);
    this.clock = Objects.requireNonNull(clock);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

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

    String requestHash = hash(command);
    Optional<ProviderRequest> existing = store.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      ProviderRequest prior = existing.get();
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
      return accepted(prior);
    }

    Optional<String> provider = RoutingPolicy.route(command.lob(), command.productClass());
    if (provider.isEmpty()) {
      throw errors
          .error(ErrorCodes.CONFIGURATION_UNRESOLVABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("HUB.ROUTING produced NO_ROUTE")
          .build();
    }

    String adapterLob =
        QuoteCanonicalMapper.requireAdapterLob(command.lob(), command.productClass()).orElseThrow();
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
            clock.instant());
    store.save(row);

    try {
      SubmitOutcome outcome =
          onesbQuotes.submitQuote(
              QuoteCanonicalMapper.toAdapterSubmit(command, adapterLob, command.actorId()));
      if (outcome instanceof SubmitOutcome.Accepted acceptedJob) {
        row.markInProgress(acceptedJob.jobId());
        store.save(row);
        return accepted(row);
      }
      row.markSubmissionUncertain();
      store.save(row);
      return accepted(row);
    } catch (ServiceException ex) {
      row.markFailed(new Failure(ex.getErrorResponse().getCode(), "adapter refused submit"));
      store.save(row);
      throw ex;
    } catch (RuntimeException ex) {
      row.markFailed(new Failure(ErrorCodes.UPSTREAM_UNAVAILABLE, "adapter submit failed"));
      store.save(row);
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("SubmitQuoteService")
          .operation("submit")
          .reason("1SB adapter submit failed")
          .build();
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

  private String hash(SubmitQuoteCommand command) {
    try {
      java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
      payload.put("operation", command.operation());
      payload.put("lob", command.lob());
      payload.put("productClass", command.productClass());
      payload.put("callerRef", command.callerRef());
      payload.put("journeyId", command.journeyId());
      payload.put("mode", command.mode());
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
