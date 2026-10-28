package com.bank.platform.integration.hub.api;

import com.bank.platform.integration.hub.application.SubmitQuoteCommand;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HTTP wire types for {@code /internal/v1/provider-requests} (QUOTE slice). */
public final class ProviderRequestHttpModels {

  private ProviderRequestHttpModels() {}

  public record CallerRefHttp(@NotBlank String context, @NotBlank String aggregateId) {}

  public record MemberHttp(
      @NotBlank String role,
      int sequence,
      @NotBlank String dob,
      @NotBlank String gender,
      Boolean tobacco,
      BigDecimal annualIncome,
      String pincode,
      String relationship) {}

  public record PreferencesHttp(String paymentFrequency, Integer policyTerm) {}

  public record SelectionHttp(
      @NotBlank String insurerCode,
      @NotEmpty List<String> productCodes,
      String planOption,
      String coverOption,
      String deathBenefitOption,
      Integer policyTerm,
      Integer premiumPaymentTerm,
      String premiumFrequency,
      String premiumPaymentOption) {}

  public record QuoteBodyHttp(
      @NotBlank String category,
      @NotNull BigDecimal requestedCover,
      BigDecimal premiumAmount,
      @NotEmpty @Valid List<MemberHttp> members,
      PreferencesHttp preferences) {}

  public record QuoteRequestHttp(
      @NotBlank String operation,
      @NotBlank String lob,
      @NotBlank String productClass,
      @NotNull @Valid CallerRefHttp callerRef,
      @NotBlank String journeyId,
      @NotBlank String mode,
      List<String> insurerCodes,
      @NotNull @Valid QuoteBodyHttp body,
      SelectionHttp selection) {}

  static SubmitQuoteCommand toCommand(
      QuoteRequestHttp request,
      String idempotencyKey,
      String actorId,
      String correlationId,
      String callerServiceId,
      JsonNode rawTree) {
    List<SubmitQuoteCommand.Member> members = new ArrayList<>();
    for (MemberHttp member : request.body().members()) {
      members.add(
          new SubmitQuoteCommand.Member(
              member.role(),
              member.sequence(),
              member.dob(),
              member.gender(),
              member.tobacco(),
              member.annualIncome(),
              member.pincode(),
              member.relationship()));
    }
    SubmitQuoteCommand.Preferences preferences = null;
    if (request.body().preferences() != null) {
      preferences =
          new SubmitQuoteCommand.Preferences(
              request.body().preferences().paymentFrequency(),
              request.body().preferences().policyTerm());
    }
    SubmitQuoteCommand.Selection selection = null;
    if (request.selection() != null) {
      selection =
          new SubmitQuoteCommand.Selection(
              request.selection().insurerCode(),
              request.selection().productCodes(),
              request.selection().planOption(),
              request.selection().coverOption(),
              request.selection().deathBenefitOption(),
              request.selection().policyTerm(),
              request.selection().premiumPaymentTerm(),
              request.selection().premiumFrequency(),
              request.selection().premiumPaymentOption());
    }
    return new SubmitQuoteCommand(
        idempotencyKey,
        actorId,
        correlationId,
        callerServiceId,
        request.operation(),
        request.lob(),
        request.productClass(),
        new SubmitQuoteCommand.CallerRefView(
            request.callerRef().context(), request.callerRef().aggregateId()),
        request.journeyId(),
        request.mode(),
        request.body().category(),
        request.body().requestedCover(),
        request.body().premiumAmount(),
        List.copyOf(members),
        preferences,
        selection,
        jsonToMap(rawTree));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> jsonToMap(JsonNode node) {
    if (node == null || node.isNull()) {
      return Map.of();
    }
    if (node.isObject()) {
      Map<String, Object> map = new LinkedHashMap<>();
      node.fields().forEachRemaining(entry -> map.put(entry.getKey(), jsonValue(entry.getValue())));
      return map;
    }
    return Map.of();
  }

  private static Object jsonValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isObject()) {
      return jsonToMap(node);
    }
    if (node.isArray()) {
      List<Object> list = new ArrayList<>();
      node.forEach(child -> list.add(jsonValue(child)));
      return list;
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isNumber()) {
      return node.decimalValue();
    }
    return node.asText();
  }
}
