package com.bank.workforce.bff.api;

import com.bank.workforce.bff.application.LoginService;
import com.bank.workforce.bff.application.SessionInvalidException;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session lifecycle and Keycloak partner OIDC begin/callback. NIP password login (Bank RM /
 * Insurance RM) is {@link LoginJourneyController} only ({@code SUG-20261028-1lg}).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

  private final LoginService loginService;
  private final WorkforceSessionProperties properties;

  public AuthenticationController(
      LoginService loginService, WorkforceSessionProperties properties) {
    this.loginService = loginService;
    this.properties = properties;
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(HttpServletRequest request) {
    CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return new CsrfResponse(token.getHeaderName(), token.getToken());
  }

  /**
   * Starts Keycloak OIDC + PKCE for an insurer partner representative (UC-02). Not the Login BRD
   * password form.
   */
  @PostMapping("/partner/authorization")
  public PartnerAuthorizationResponse partnerAuthorization(
      @Valid @RequestBody PartnerAuthorizationRequest request) {
    var result =
        loginService.begin(
            new LoginService.BeginLoginCommand(
                request.clientType(), request.returnUri(), request.loginHint()));
    return new PartnerAuthorizationResponse(result.authorizationUri(), result.expiresInSeconds());
  }

  @GetMapping("/callback")
  public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
    var completion = loginService.complete(state, code);
    URI redirect =
        completion.clientType() == ClientType.NATIVE
            ? appendQuery(completion.returnUri(), "code", completion.nativeCompletionCode())
            : URI.create(completion.returnUri());
    ResponseEntity.BodyBuilder response =
        ResponseEntity.status(HttpStatus.FOUND).location(redirect);
    if (completion.clientType() == ClientType.WEB) {
      response.header(
          HttpHeaders.SET_COOKIE,
          WorkforceSessionCookie.issue(properties, completion.browserSessionId()).toString());
    }
    return response.build();
  }

  @PostMapping("/native-session")
  public NativeSessionResponse nativeSession(@Valid @RequestBody NativeSessionRequest request) {
    return new NativeSessionResponse(
        loginService.exchangeNativeCompletion(request.completionCode()),
        properties.sessionTtl().toSeconds());
  }

  @GetMapping("/session")
  public SessionResponse session(
      @RequestHeader(value = "X-Session-Handle", required = false) String nativeHandle,
      @CookieValue(value = "${workforce.session.cookie-name:WORKFORCE_SESSION}", required = false)
          String browserHandle) {
    var session = loginService.session(requireHandle(nativeHandle, browserHandle));
    return new SessionResponse(
        session.businessUserId(),
        session.username(),
        session.userType(),
        session.status(),
        session.policyVersion(),
        session.insurerCode());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader(value = "X-Session-Handle", required = false) String nativeHandle,
      @CookieValue(value = "${workforce.session.cookie-name:WORKFORCE_SESSION}", required = false)
          String browserHandle) {
    loginService.logout(requireHandle(nativeHandle, browserHandle));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, WorkforceSessionCookie.clear(properties).toString())
        .build();
  }

  private String requireHandle(String nativeHandle, String browserHandle) {
    String handle = nativeHandle != null && !nativeHandle.isBlank() ? nativeHandle : browserHandle;
    if (handle == null || handle.isBlank()) {
      throw new SessionInvalidException("Session handle is required");
    }
    return handle;
  }

  private static URI appendQuery(String returnUri, String name, String value) {
    String separator = returnUri.contains("?") ? "&" : "?";
    return URI.create(returnUri + separator + name + "=" + value);
  }

  public record PartnerAuthorizationRequest(
      @NotNull ClientType clientType, @NotBlank String returnUri, String loginHint) {}

  public record PartnerAuthorizationResponse(URI authorizationUri, long expiresInSeconds) {}

  public record NativeSessionRequest(@NotBlank String completionCode) {}

  public record NativeSessionResponse(String sessionHandle, long expiresInSeconds) {}

  public record CsrfResponse(String headerName, String token) {}

  public record SessionResponse(
      UUID businessUserId,
      String username,
      String userType,
      String status,
      long policyVersion,
      String insurerCode) {}
}
