package com.bank.platform.integration.hub.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.integration.hub.application.QuoteCanonicalMapper.MappedJob;
import com.bank.platform.integration.hub.domain.CallerContexts;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FUNC-032 — poll canonical provider-request status; advances IN_PROGRESS / uncertain. */
@Service
public class GetProviderRequestService {

  private final ProviderRequestStore store;
  private final OnesbQuotePort onesbQuotes;
  private final ServiceErrors errors;
  private final Clock clock;
  private final Duration uncertainWindow;

  public GetProviderRequestService(
      ProviderRequestStore store,
      OnesbQuotePort onesbQuotes,
      ServiceErrors errors,
      Clock clock,
      @Value("${hub.reconciliation.uncertain-window:PT10M}") Duration uncertainWindow) {
    this.store = Objects.requireNonNull(store);
    this.onesbQuotes = Objects.requireNonNull(onesbQuotes);
    this.errors = Objects.requireNonNull(errors);
    this.clock = Objects.requireNonNull(clock);
    this.uncertainWindow = Objects.requireNonNull(uncertainWindow);
  }

  @Transactional
  public ProviderRequestStatusView get(String providerRequestId, String callerServiceId) {
    Optional<ProviderRequest> found = store.findById(providerRequestId);
    if (found.isEmpty()) {
      throw notFound();
    }
    ProviderRequest row = found.get();
    Optional<String> context = CallerContexts.contextForService(callerServiceId);
    if (context.isEmpty() || !context.get().equals(row.callerRef().context())) {
      throw notFound();
    }

    if (row.state() == CanonicalState.SUBMISSION_UNCERTAIN) {
      if (!clock.instant().isBefore(row.updatedAt().plus(uncertainWindow))) {
        row.markFailed(
            new Failure(ErrorCodes.UPSTREAM_TIMEOUT, "uncertain reconciliation window elapsed"),
            clock.instant());
        store.save(row);
      }
    } else if (row.state() == CanonicalState.IN_PROGRESS && row.adapterJobId() != null) {
      MappedJob mapped = QuoteCanonicalMapper.mapJob(onesbQuotes.getQuoteJob(row.adapterJobId()));
      if (mapped.state() != CanonicalState.IN_PROGRESS) {
        row.applyAdapterJob(mapped.state(), mapped.result(), mapped.failure(), clock.instant());
        store.save(row);
      }
    } else if (row.adapterJobId() != null
        && (row.state() == CanonicalState.COMPLETED
            || row.state() == CanonicalState.PARTIALLY_COMPLETED)) {
      // LLD §10 — offers not stored; reload from adapter for the response.
      MappedJob mapped = QuoteCanonicalMapper.mapJob(onesbQuotes.getQuoteJob(row.adapterJobId()));
      row.attachResult(mapped.result());
    }

    return new ProviderRequestStatusView(
        row.providerRequestId(),
        row.operation(),
        row.state().name(),
        row.provider(),
        row.callerRef(),
        row.createdAt(),
        row.result(),
        row.failure());
  }

  private RuntimeException notFound() {
    return errors
        .error(ErrorCodes.RESOURCE_NOT_FOUND)
        .component("GetProviderRequestService")
        .operation("get")
        .reason("provider request not found")
        .build();
  }
}
