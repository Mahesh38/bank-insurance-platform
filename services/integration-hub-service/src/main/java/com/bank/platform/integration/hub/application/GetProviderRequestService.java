package com.bank.platform.integration.hub.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.integration.hub.application.QuoteCanonicalMapper.MappedJob;
import com.bank.platform.integration.hub.domain.CallerContexts;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** FUNC-032 — poll canonical provider-request status; advances IN_PROGRESS via adapter GET. */
@Service
public class GetProviderRequestService {

  private final ProviderRequestStore store;
  private final OnesbQuotePort onesbQuotes;
  private final ServiceErrors errors;

  public GetProviderRequestService(
      ProviderRequestStore store, OnesbQuotePort onesbQuotes, ServiceErrors errors) {
    this.store = Objects.requireNonNull(store);
    this.onesbQuotes = Objects.requireNonNull(onesbQuotes);
    this.errors = Objects.requireNonNull(errors);
  }

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
    if (row.state() == CanonicalState.IN_PROGRESS && row.adapterJobId() != null) {
      MappedJob mapped = QuoteCanonicalMapper.mapJob(onesbQuotes.getQuoteJob(row.adapterJobId()));
      if (mapped.state() != CanonicalState.IN_PROGRESS) {
        row.applyAdapterJob(mapped.state(), mapped.result(), mapped.failure());
        store.save(row);
      }
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
