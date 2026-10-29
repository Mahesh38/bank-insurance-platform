package com.bank.workforce.bff.api;

import com.bank.workforce.bff.application.login.LoginJourneyService;
import com.bank.workforce.bff.application.login.LoginJourneyService.JourneyBody;
import com.bank.workforce.bff.application.login.LoginJourneyService.SignInCommand;
import com.bank.workforce.bff.application.login.LoginJourneyService.UnlockCommand;
import com.bank.workforce.bff.application.login.LoginJourneyService.WorkforceLoginUser;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login BRD v3.1 — the only NIP password-login surface ({@code SUG-20261028-1lg}). Design sentences
 * come back on {@code outcome=MESSAGE}. A session cookie is set only after a successful login OTP.
 * IAM-001 immediate-session login and {@code /sign-in} are retired.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class LoginJourneyController {

  private final LoginJourneyService journey;
  private final WorkforceSessionProperties properties;

  public LoginJourneyController(
      LoginJourneyService journey, WorkforceSessionProperties properties) {
    this.journey = journey;
    this.properties = properties;
  }

  @PostMapping("/captcha")
  public JourneyBody captcha() {
    return journey.issueCaptcha().body();
  }

  @PostMapping("/login")
  public ResponseEntity<JourneyBody> login(@Valid @RequestBody SignInRequest request) {
    return respond(
        journey.signIn(
            new SignInCommand(
                request.clientType(),
                request.userType(),
                request.loginId(),
                request.password(),
                request.captchaId(),
                request.captchaValue())));
  }

  @PostMapping("/otp/verify")
  public ResponseEntity<JourneyBody> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
    return respond(journey.verifyOtp(request.challengeId(), request.otp()));
  }

  @PostMapping("/otp/resend")
  public JourneyBody resend(@Valid @RequestBody OtpResendRequest request) {
    return journey.resend(request.challengeId()).body();
  }

  @PostMapping("/unlock")
  public JourneyBody unlock(@Valid @RequestBody UnlockRequest request) {
    return journey
        .unlock(
            new UnlockCommand(
                request.clientType(),
                request.userType(),
                request.loginId(),
                request.captchaId(),
                request.captchaValue()))
        .body();
  }

  @PostMapping("/forgot-password")
  public JourneyBody forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return journey
        .forgotPassword(
            request.clientType(), request.email(), request.captchaId(), request.captchaValue())
        .body();
  }

  @PostMapping("/password-expired/send-otp")
  public JourneyBody sendExpiredOtp(@Valid @RequestBody ExpiryOtpRequest request) {
    return journey.sendExpiredPasswordOtp(request.expiryTicket()).body();
  }

  @PostMapping("/password")
  public JourneyBody setPassword(@Valid @RequestBody SetPasswordRequest request) {
    return journey
        .setPassword(request.resetToken(), request.newPassword(), request.confirmPassword())
        .body();
  }

  private ResponseEntity<JourneyBody> respond(LoginJourneyService.JourneyResult result) {
    ResponseEntity.BodyBuilder response = ResponseEntity.ok();
    if (result.browserSessionId() != null) {
      response.header(HttpHeaders.SET_COOKIE, sessionCookie(result.browserSessionId()).toString());
    }
    return response.body(result.body());
  }

  private ResponseCookie sessionCookie(String value) {
    return ResponseCookie.from(properties.cookieName(), value)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(properties.sessionTtl())
        .build();
  }

  public record SignInRequest(
      @NotNull ClientType clientType,
      @NotNull WorkforceLoginUser userType,
      @NotBlank String loginId,
      @NotBlank String password,
      @NotBlank String captchaId,
      @NotBlank String captchaValue) {
    @Override
    public String toString() {
      return "SignInRequest[clientType=" + clientType + ", userType=" + userType + "]";
    }
  }

  public record UnlockRequest(
      @NotNull ClientType clientType,
      @NotNull WorkforceLoginUser userType,
      @NotBlank String loginId,
      @NotBlank String captchaId,
      @NotBlank String captchaValue) {}

  public record ForgotPasswordRequest(
      @NotNull ClientType clientType,
      @NotBlank String email,
      @NotBlank String captchaId,
      @NotBlank String captchaValue) {}

  public record OtpVerifyRequest(@NotBlank String challengeId, @NotBlank String otp) {
    @Override
    public String toString() {
      return "OtpVerifyRequest[challengeId=" + challengeId + "]";
    }
  }

  public record OtpResendRequest(@NotBlank String challengeId) {}

  public record ExpiryOtpRequest(@NotBlank String expiryTicket) {}

  public record SetPasswordRequest(
      @NotBlank String resetToken, @NotBlank String newPassword, @NotBlank String confirmPassword) {
    @Override
    public String toString() {
      return "SetPasswordRequest[resetToken]";
    }
  }
}
