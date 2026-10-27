package com.bank.platform.consent.api;

import com.bank.platform.consent.application.ConsentLinkService;
import com.bank.platform.consent.application.ConsentLinkService.IssueCommand;
import com.bank.platform.consent.application.ConsentLinkService.LinkView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Seller and suitability calls. The response never contains the raw token. */
@RestController
@RequestMapping("/internal/v1/consent-links")
public class ConsentLinkController {

  private final ConsentLinkService links;
  private final ActorGuard actors;

  public ConsentLinkController(ConsentLinkService links, ActorGuard actors) {
    this.links = links;
    this.actors = actors;
  }

  @GetMapping("/quote-share")
  public QuoteShareView quoteShare(@RequestParam String customerId) {
    return new QuoteShareView(links.holdsQuoteShare(customerId));
  }

  @PostMapping
  public ResponseEntity<LinkView> issue(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @Valid @RequestBody IssueBody body) {
    String actor = actors.requireActorId(request, actorId, "issueConsentLink");
    LinkView view =
        links.issue(
            new IssueCommand(
                body.customerId(),
                body.leadId(),
                body.journeyId(),
                body.assessmentId(),
                body.registeredMobile(),
                body.registeredEmail()),
            actor);
    return ResponseEntity.status(view.sent() ? HttpStatus.CREATED : HttpStatus.OK).body(view);
  }

  @PostMapping("/resend")
  public LinkView resend(
      HttpServletRequest request,
      @RequestHeader("X-Actor-Id") String actorId,
      @Valid @RequestBody ResendBody body) {
    String actor = actors.requireActorId(request, actorId, "resendConsentLink");
    return links.resend(body.assessmentId(), actor);
  }

  @GetMapping
  public LinkView status(@RequestParam String assessmentId) {
    return links.status(assessmentId);
  }

  public record IssueBody(
      @NotBlank String customerId,
      @NotBlank String leadId,
      String journeyId,
      @NotBlank String assessmentId,
      @NotBlank String registeredMobile,
      @NotBlank String registeredEmail) {}

  public record ResendBody(@NotBlank String assessmentId) {}

  public record QuoteShareView(boolean granted) {}
}
