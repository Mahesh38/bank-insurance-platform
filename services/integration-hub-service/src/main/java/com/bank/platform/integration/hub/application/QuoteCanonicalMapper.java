package com.bank.platform.integration.hub.application;

import com.bank.platform.integration.hub.domain.CanonicalOffer;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterDistribution;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterMember;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterOffer;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterQuoteJob;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterQuoteSubmit;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterSelection;
import com.bank.platform.integration.hub.domain.QuoteResult;
import com.bank.platform.integration.hub.domain.RoutingPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Canonical ↔ adapter quote mapping (Hub LLD §5.1–§5.2). */
final class QuoteCanonicalMapper {

  private QuoteCanonicalMapper() {}

  static AdapterQuoteSubmit toAdapterSubmit(
      SubmitQuoteCommand command, String adapterLob, String actorId) {
    List<AdapterMember> members = new ArrayList<>();
    for (SubmitQuoteCommand.Member member : command.members()) {
      members.add(
          new AdapterMember(
              member.role(),
              member.sequence(),
              member.dob(),
              member.gender(),
              member.tobacco(),
              member.annualIncome(),
              member.pincode(),
              member.relationship()));
    }
    Map<String, Object> preferences = mapPreferences(command.preferences());
    AdapterSelection selection = null;
    if (command.selection() != null) {
      SubmitQuoteCommand.Selection sel = command.selection();
      selection =
          new AdapterSelection(
              sel.insurerCode(),
              sel.productCodes(),
              sel.planOption(),
              sel.coverOption(),
              sel.deathBenefitOption(),
              sel.policyTerm(),
              sel.premiumPaymentTerm(),
              sel.premiumFrequency(),
              sel.premiumPaymentOption());
    }
    return new AdapterQuoteSubmit(
        adapterLob,
        command.mode(),
        command.category(),
        command.requestedCover(),
        command.premiumAmount(),
        List.copyOf(members),
        preferences,
        new AdapterDistribution(actorId, actorId, "B2B"),
        command.journeyId(),
        selection,
        command.idempotencyKey(),
        actorId,
        command.correlationId());
  }

  private static Map<String, Object> mapPreferences(SubmitQuoteCommand.Preferences preferences) {
    if (preferences == null) {
      return Map.of();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    if (preferences.paymentFrequency() != null) {
      String freq = preferences.paymentFrequency();
      if ("HALF_YEARLY".equalsIgnoreCase(freq)) {
        out.put("paymentFrequency", "HALF-YEARLY");
      } else {
        out.put("paymentFrequency", freq);
      }
    }
    if (preferences.policyTerm() != null) {
      out.put("policyTerm", preferences.policyTerm());
    }
    return Map.copyOf(out);
  }

  static MappedJob mapJob(AdapterQuoteJob job) {
    String status = job.status() == null ? "" : job.status().toUpperCase(Locale.ROOT);
    return switch (status) {
      case "PENDING", "RUNNING" -> new MappedJob(CanonicalState.IN_PROGRESS, null, null);
      case "COMPLETED" -> new MappedJob(CanonicalState.COMPLETED, toResult(job, false), null);
      case "PARTIAL" ->
          new MappedJob(CanonicalState.PARTIALLY_COMPLETED, toResult(job, true), null);
      case "FAILED" ->
          new MappedJob(
              CanonicalState.FAILED,
              null,
              new Failure("UPSTREAM_BAD_RESPONSE", job.failureReason()));
      case "TIMEOUT" ->
          new MappedJob(
              CanonicalState.TIMED_OUT, null, new Failure("QUOTE_TIMEOUT", job.failureReason()));
      default ->
          new MappedJob(
              CanonicalState.FAILED,
              null,
              new Failure("UPSTREAM_BAD_RESPONSE", "unrecognised adapter status"));
    };
  }

  private static QuoteResult toResult(AdapterQuoteJob job, boolean partial) {
    List<CanonicalOffer> offers = new ArrayList<>();
    List<QuoteResult.PartialError> errors = new ArrayList<>();
    for (AdapterOffer offer : job.offers() == null ? List.<AdapterOffer>of() : job.offers()) {
      // LLD §5.5 — errorSummary rows are partialErrors only, never offers.
      if (offer.errorSummary() != null && !offer.errorSummary().isBlank()) {
        errors.add(
            new QuoteResult.PartialError(
                offer.insurerCode(),
                offer.productCode(),
                "UPSTREAM_BUSINESS_ERROR",
                offer.errorSummary()));
        continue;
      }
      if (offer.offerId() == null || offer.offerId().isBlank()) {
        continue;
      }
      offers.add(
          new CanonicalOffer(
              offer.offerId(),
              offer.insurerCode(),
              offer.insurerName(),
              offer.productCode(),
              offer.productName(),
              offer.premiumAmount(),
              offer.premiumFrequency(),
              offer.sumAssured(),
              offer.outOfBound(),
              offer.coverTerm(),
              offer.premiumPayingTerm(),
              offer.funds()));
    }
    if (!partial && errors.isEmpty()) {
      return new QuoteResult(offers, List.of());
    }
    return new QuoteResult(offers, errors);
  }

  static Optional<String> requireAdapterLob(String lob, String productClass) {
    return RoutingPolicy.adapterLob(lob, productClass);
  }

  record MappedJob(CanonicalState state, QuoteResult result, Failure failure) {}
}
