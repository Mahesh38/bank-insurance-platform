package com.bank.platform.consent.api;

import com.bank.platform.consent.application.ConsentApplicationService;
import com.bank.platform.consent.application.ConsentApplicationService.Affirmation;
import com.bank.platform.consent.application.ConsentApplicationService.CaptureCommand;
import com.bank.platform.consent.application.ConsentApplicationService.VerifyCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contract operations from the consent OpenAPI. */
@RestController
@RequestMapping("/internal/v1")
public class ConsentController {

  private final ConsentApplicationService consents;
  private final IdempotentExecutor idempotency;

  public ConsentController(ConsentApplicationService consents, IdempotentExecutor idempotency) {
    this.consents = consents;
    this.idempotency = idempotency;
  }

  @GetMapping("/consents:check")
  public ConsentApplicationService.CheckView check(
      @RequestParam String customerId,
      @RequestParam String checkpoint,
      @RequestParam String lob,
      @RequestParam(required = false) String insurerCode,
      @RequestParam(required = false) String journeyId) {
    return consents.check(customerId, checkpoint, lob, insurerCode, journeyId);
  }

  @PostMapping("/consents")
  public ResponseEntity<String> capture(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @Valid @RequestBody CaptureBody body) {
    return idempotency.execute(
        key,
        body,
        () ->
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    consents.capture(
                        new CaptureCommand(
                            body.customerId(),
                            body.leadId(),
                            body.journeyId(),
                            body.lob(),
                            body.checkpoint(),
                            body.purposes(),
                            body.insurerCode(),
                            body.channel(),
                            body.language()),
                        actorId,
                        correlationId)));
  }

  @GetMapping("/consents")
  public List<ConsentApplicationService.ChainView> list(
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String journeyId) {
    return consents.list(customerId, journeyId);
  }

  @GetMapping("/consent-challenges/{challengeId}")
  public ConsentApplicationService.ChallengeView challenge(@PathVariable String challengeId) {
    return consents.getChallenge(challengeId);
  }

  @PostMapping("/consent-challenges/{challengeId}/verify")
  public ResponseEntity<String> verify(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @PathVariable String challengeId,
      @Valid @RequestBody VerifyBody body) {
    return idempotency.execute(
        key,
        body,
        () ->
            ResponseEntity.ok(
                consents.verify(
                    new VerifyCommand(
                        challengeId,
                        body.otpTxnId(),
                        body.otp(),
                        body.verificationMode(),
                        body.sourceIp(),
                        body.affirmations() == null
                            ? List.of()
                            : body.affirmations().stream()
                                .map(
                                    item ->
                                        new Affirmation(
                                            item.purpose(),
                                            item.statementId(),
                                            item.statementVersion(),
                                            item.affirmedAt()))
                                .toList()),
                    actorId,
                    correlationId)));
  }

  @PostMapping("/consent-challenges/{challengeId}/otp:resend")
  public ResponseEntity<String> resend(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @PathVariable String challengeId) {
    return idempotency.execute(
        key,
        challengeId,
        () -> ResponseEntity.ok(consents.resend(challengeId, actorId, correlationId)));
  }

  @GetMapping("/consents/{consentId}")
  public ConsentApplicationService.ConsentView consent(
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String consentId,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String journeyId) {
    return consents.getConsent(consentId, customerId, actorId, journeyId);
  }

  @PostMapping("/consents/{consentId}/withdrawals")
  public ResponseEntity<String> withdraw(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @PathVariable String consentId,
      @Valid @RequestBody WithdrawalBody body) {
    return idempotency.execute(
        key,
        body,
        () ->
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    consents.startWithdrawal(
                        consentId, body.channel(), body.reasonCode(), actorId, correlationId)));
  }

  public record CaptureBody(
      @NotBlank String customerId,
      @NotBlank String leadId,
      String journeyId,
      @NotBlank String lob,
      @NotBlank String checkpoint,
      @NotEmpty List<String> purposes,
      String insurerCode,
      @NotBlank String channel,
      @NotBlank String language) {}

  public record VerifyBody(
      @NotBlank String otpTxnId,
      @NotBlank String otp,
      @NotBlank String verificationMode,
      String sourceIp,
      List<AffirmationBody> affirmations) {}

  public record AffirmationBody(
      String purpose, String statementId, String statementVersion, Instant affirmedAt) {}

  public record WithdrawalBody(@NotBlank String channel, String reasonCode) {}
}
