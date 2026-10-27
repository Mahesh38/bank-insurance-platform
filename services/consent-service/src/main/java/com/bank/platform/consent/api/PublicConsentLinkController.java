package com.bank.platform.consent.api;

import com.bank.platform.consent.application.ConsentLinkService;
import com.bank.platform.consent.application.ConsentLinkService.PublicPage;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public customer page. Authentication is the unguessable token, not a workforce session. */
@RestController
@RequestMapping("/public/v1/consent-links")
public class PublicConsentLinkController {

  private final ConsentLinkService links;

  public PublicConsentLinkController(ConsentLinkService links) {
    this.links = links;
  }

  @GetMapping(path = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> open(@PathVariable String token) {
    return html(links.open(token));
  }

  @PostMapping(
      path = "/{token}",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> decideForm(
      @PathVariable String token,
      @RequestParam(required = false) String decision,
      HttpServletRequest request) {
    return html(links.decide(token, decision, request.getRemoteAddr()));
  }

  @PostMapping(
      path = "/{token}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> decideJson(
      @PathVariable String token, @RequestBody DecisionBody body, HttpServletRequest request) {
    PublicPage page =
        links.decide(token, body == null ? null : body.decision(), request.getRemoteAddr());
    return json(page);
  }

  private static ResponseEntity<String> html(PublicPage page) {
    return ResponseEntity.status(status(page.kind()))
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .header("Referrer-Policy", "no-referrer")
        .header("X-Frame-Options", "DENY")
        .header(
            "Content-Security-Policy",
            "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")
        .contentType(MediaType.TEXT_HTML)
        .body(ConsentPageHtml.render(page.kind(), page.statement()));
  }

  private static ResponseEntity<Map<String, String>> json(PublicPage page) {
    String statement = page.statement() == null ? "" : page.statement();
    return ResponseEntity.status(status(page.kind()))
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .header("Referrer-Policy", "no-referrer")
        .body(Map.of("kind", page.kind(), "statement", statement));
  }

  static HttpStatus status(String kind) {
    return switch (kind) {
      case "INVALID" -> HttpStatus.NOT_FOUND;
      case "EXPIRED" -> HttpStatus.GONE;
      case "USED" -> HttpStatus.CONFLICT;
      default -> HttpStatus.OK;
    };
  }

  public record DecisionBody(String decision) {}
}
