package com.bank.platform.integration.hub.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.integration.hub.api.ProviderRequestHttpModels.QuoteRequestHttp;
import com.bank.platform.integration.hub.application.GetProviderRequestService;
import com.bank.platform.integration.hub.application.ProviderRequestAccepted;
import com.bank.platform.integration.hub.application.ProviderRequestStatusView;
import com.bank.platform.integration.hub.application.SubmitQuoteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canonical provider-request API — FUNC-032 QUOTE submit/poll. Path matches Hub OpenAPI {@code
 * /internal/v1/provider-requests}.
 */
@RestController
@RequestMapping("/internal/v1/provider-requests")
public class ProviderRequestController {

  private final SubmitQuoteService submitQuotes;
  private final GetProviderRequestService getRequests;
  private final ActorGuard actors;
  private final ServiceErrors errors;
  private final ObjectMapper objectMapper;
  private final Validator validator;

  public ProviderRequestController(
      SubmitQuoteService submitQuotes,
      GetProviderRequestService getRequests,
      ActorGuard actors,
      ServiceErrors errors,
      ObjectMapper objectMapper,
      Validator validator) {
    this.submitQuotes = Objects.requireNonNull(submitQuotes);
    this.getRequests = Objects.requireNonNull(getRequests);
    this.actors = Objects.requireNonNull(actors);
    this.errors = Objects.requireNonNull(errors);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.validator = Objects.requireNonNull(validator);
  }

  @PostMapping
  public ResponseEntity<ProviderRequestAccepted> submit(
      HttpServletRequest httpRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @RequestBody JsonNode rawBody) {
    String caller = actors.requireCaller(httpRequest, "submitProviderRequest");
    String actor = actors.requireActorId(httpRequest, actorId, "submitProviderRequest");
    if (correlationId == null || correlationId.isBlank()) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("ProviderRequestController")
          .operation("submitProviderRequest")
          .reason("X-Correlation-Id is required")
          .build();
    }
    if (rawBody == null || !rawBody.isObject()) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("ProviderRequestController")
          .operation("submitProviderRequest")
          .reason("body must be a JSON object")
          .build();
    }
    String operation = rawBody.path("operation").asText("");
    if (!"QUOTE".equals(operation)) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("ProviderRequestController")
          .operation("submitProviderRequest")
          .reason("FUNC-032 supports operation QUOTE only")
          .build();
    }
    QuoteRequestHttp request = objectMapper.convertValue(rawBody, QuoteRequestHttp.class);
    Set<ConstraintViolation<QuoteRequestHttp>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      String detail =
          violations.stream()
              .map(v -> v.getPropertyPath() + " " + v.getMessage())
              .collect(Collectors.joining("; "));
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component("ProviderRequestController")
          .operation("submitProviderRequest")
          .reason(detail)
          .build();
    }
    ProviderRequestAccepted accepted =
        submitQuotes.submit(
            ProviderRequestHttpModels.toCommand(
                request, idempotencyKey, actor, correlationId.trim(), caller, rawBody));
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted);
  }

  /**
   * Poll advances {@code IN_PROGRESS} / {@code SUBMISSION_UNCERTAIN} per Hub LLD §4.2. Callers are
   * internal services authenticated with {@code X-Internal-Service-Key} (not browser cookie
   * sessions), so CSRF protections that apply to cookie clients (VR-019) do not apply here.
   */
  // lgtm[java/spring-csrf-unprotected-request-type]
  // codeql[java/spring-csrf-unprotected-request-type]
  @GetMapping("/{providerRequestId}")
  public ResponseEntity<Map<String, Object>> get(
      HttpServletRequest httpRequest, @PathVariable String providerRequestId) {
    String caller = actors.requireCaller(httpRequest, "getProviderRequest");
    ProviderRequestStatusView status = getRequests.get(providerRequestId, caller);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("providerRequestId", status.providerRequestId());
    body.put("operation", status.operation());
    body.put("state", status.state());
    body.put("provider", status.provider());
    body.put(
        "callerRef",
        Map.of(
            "context", status.callerRef().context(),
            "aggregateId", status.callerRef().aggregateId()));
    body.put("createdAt", status.createdAt().toString());
    if (status.result() != null) {
      body.put("result", status.result());
    }
    if (status.failure() != null) {
      body.put("failure", status.failure());
    }
    return ResponseEntity.ok(body);
  }
}
