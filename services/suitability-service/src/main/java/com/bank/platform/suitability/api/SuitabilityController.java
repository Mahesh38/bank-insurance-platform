package com.bank.platform.suitability.api;

import com.bank.platform.suitability.application.AssessmentView;
import com.bank.platform.suitability.application.SuitabilityJourney;
import com.bank.platform.suitability.application.SuitabilityJourney.CreateCommand;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Seller journey for the current-design suitability screens, including section 9.10. */
@RestController
@RequestMapping("/internal/v1/suitability/assessments")
public class SuitabilityController {

  private final SuitabilityJourney journey;
  private final ActorGuard actors;

  public SuitabilityController(SuitabilityJourney journey, ActorGuard actors) {
    this.journey = journey;
    this.actors = actors;
  }

  @PostMapping
  public ResponseEntity<AssessmentView> create(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @Valid @RequestBody CreateBody body) {
    String actor = actors.requireActorId(request, actorId, "createAssessment");
    AssessmentView view =
        journey.create(
            new CreateCommand(
                body.leadId(),
                body.journeyId(),
                body.customerId(),
                body.productType(),
                body.proposerName(),
                body.proposerDob(),
                body.registeredMobile(),
                body.registeredEmail(),
                body.spActorId(),
                body.rmActorId()),
            actor);
    return ResponseEntity.status(HttpStatus.CREATED).body(view);
  }

  @GetMapping("/{id}")
  public AssessmentView get(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id) {
    String actor = actors.requireActorId(request, actorId, "readAssessment");
    return journey.get(id, actor);
  }

  @PutMapping("/{id}/answers")
  public AssessmentView answers(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id,
      @RequestBody JsonNode body) {
    String actor = actors.requireActorId(request, actorId, "saveAnswers");
    return journey.saveAnswers(id, body, actor);
  }

  @PostMapping("/{id}/save-exit")
  public AssessmentView saveExit(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id) {
    String actor = actors.requireActorId(request, actorId, "saveExit");
    return journey.saveExit(id, actor);
  }

  @PostMapping("/{id}/explore-matching-plans")
  public AssessmentView explore(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id) {
    String actor = actors.requireActorId(request, actorId, "exploreMatchingPlans");
    return journey.explore(id, actor);
  }

  @PostMapping("/{id}/consent-link:resend")
  public AssessmentView resend(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id) {
    String actor = actors.requireActorId(request, actorId, "resendConsentLink");
    return journey.resend(id, actor);
  }

  @PostMapping("/{id}/final-bi")
  public AssessmentView finalBi(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String id,
      @Valid @RequestBody FinalBiBody body) {
    String actor = actors.requireActorId(request, actorId, "lockFinalBi");
    return journey.finalBi(id, body.biReference(), actor);
  }

  public record CreateBody(
      @NotBlank String leadId,
      @NotBlank String journeyId,
      @NotBlank String customerId,
      @NotBlank String productType,
      String proposerName,
      String proposerDob,
      String registeredMobile,
      String registeredEmail,
      @NotBlank String spActorId,
      @NotBlank String rmActorId) {}

  public record FinalBiBody(@NotBlank String biReference) {}
}
