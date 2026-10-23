package com.bank.workforce.bff.application.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.workforce.bff.application.LoginService;
import com.bank.workforce.bff.application.login.LoginJourneyService.JourneyBody;
import com.bank.workforce.bff.application.login.LoginJourneyService.SignInCommand;
import com.bank.workforce.bff.application.login.LoginJourneyService.UnlockCommand;
import com.bank.workforce.bff.application.login.LoginJourneyService.WorkforceLoginUser;
import com.bank.workforce.bff.client.IdentityAuthorizationClient;
import com.bank.workforce.bff.client.IdentityAuthorizationClient.AccountView;
import com.bank.workforce.bff.client.IdentityAuthorizationClient.PasswordOutcome;
import com.bank.workforce.bff.client.IdentityProviderClient;
import com.bank.workforce.bff.config.LoginJourneyProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginJourneyServiceTest {

  private static final UUID USER = UUID.fromString("39c1b574-df9e-43b4-bca8-38400f3d3410");

  @Mock IdentityAuthorizationClient authorization;
  @Mock IdentityProviderClient provider;
  @Mock LoginService sessions;

  private final InMemoryChallengeStore challenges = new InMemoryChallengeStore();
  private final MutableClock clock = new MutableClock(Instant.parse("2026-10-23T12:00:00Z"));
  private final SequenceCodes codes =
      new SequenceCodes("111111", "222222", "333333", "444444", "555555");
  private LoginJourneyService journey;

  @BeforeEach
  void setUp() {
    journey =
        new LoginJourneyService(
            authorization,
            provider,
            sessions,
            challenges,
            codes,
            new LoginJourneyProperties(
                Duration.ofMinutes(5),
                Duration.ofMinutes(10),
                Duration.ofMinutes(2),
                5,
                3,
                Duration.ofMinutes(10)),
            clock);
  }

  @Test
  void wrongCaptchaDoesNotCheckThePassword() {
    JourneyBody captcha = journey.issueCaptcha().body();
    JourneyBody result =
        journey
            .signIn(
                signIn(WorkforceLoginUser.BANK_RM, "E100", "secret", captcha.captchaId(), "nope"))
            .body();

    assertThat(result.outcome()).isEqualTo("MESSAGE");
    assertThat(result.code()).isEqualTo("VAL-004");
    assertThat(result.message()).isEqualTo("Wrong captcha. Try again.");
    verify(authorization, never()).preview(anyString(), anyString());
    verify(provider, never()).verifyAd(anyString(), anyString());
  }

  @Test
  void unknownEmployeeAndFirstTimeStopBeforeThePassword() {
    when(authorization.preview("BANK_EMPLOYEE", "E404")).thenReturn(account("UNKNOWN", 3));
    JourneyBody unknown = signAs(WorkforceLoginUser.BANK_RM, "E404", "secret");
    assertThat(unknown.code()).isEqualTo("VAL-005");

    when(authorization.preview("BANK_EMPLOYEE", "E100")).thenReturn(account("FIRST_TIME", 3));
    JourneyBody first = signAs(WorkforceLoginUser.BANK_RM, "E100", "secret");
    assertThat(first.message()).contains("Please use Unlock User");
    verify(provider, never()).verifyAd(anyString(), anyString());
    verify(authorization, never()).recordBankPassword(any(), anyBoolean());
  }

  @Test
  void thirdBankPasswordFailureLocksAndACorrectPasswordOnlyIssuesAnOtp() {
    when(authorization.preview("BANK_EMPLOYEE", "E100")).thenReturn(account("READY", 3));
    when(provider.verifyAd("E100", "secret"))
        .thenReturn(new IdentityProviderClient.AdVerifyResult(false, true, "E100", "E100", "e@x"));
    when(authorization.recordBankPassword(USER, false))
        .thenReturn(new PasswordOutcome(account("PASSWORD_REJECTED", 2), false))
        .thenReturn(new PasswordOutcome(account("PASSWORD_REJECTED", 1), false))
        .thenReturn(new PasswordOutcome(account("LOCKED_PASSWORD", 0), false));

    assertThat(sign("E100").message())
        .isEqualTo("Incorrect password. You have 2 attempts remaining.");
    assertThat(sign("E100").message())
        .isEqualTo("Incorrect password. You have 1 attempt remaining.");
    JourneyBody locked = sign("E100");
    assertThat(locked.code()).isEqualTo("VAL-010");
    assertThat(locked.message())
        .doesNotContain("Your account has not been locked")
        .contains("Unlock User");

    when(provider.verifyAd("E100", "secret"))
        .thenReturn(new IdentityProviderClient.AdVerifyResult(true, true, "E100", "E100", "e@x"));
    when(authorization.recordBankPassword(USER, true))
        .thenReturn(new PasswordOutcome(account("READY", 3), true));
    JourneyBody otp = sign("E100");
    assertThat(otp.outcome()).isEqualTo("OTP_SENT");
    assertThat(otp.maskedMobile()).isEqualTo("xx1234");
    assertThat(otp.toString()).doesNotContain("111111");
    verify(sessions, never()).establishSession(any(), any());
  }

  @Test
  void loginOtpCreatesOneSessionAndASecondSubmitDoesNot() {
    String challenge = issuedLoginOtp();
    when(authorization.completeLogin(USER)).thenReturn(account("READY", 3));
    when(sessions.establishSession(any(), any()))
        .thenReturn(
            new LoginService.BankLoginResult(
                ClientType.WEB,
                true,
                USER,
                "BANK_EMPLOYEE",
                "ACTIVE",
                1,
                "browser-session",
                null,
                28800));

    var first = journey.verifyOtp(challenge, "111111");
    assertThat(first.body().outcome()).isEqualTo("AUTHENTICATED");
    assertThat(first.body().title()).isEqualTo("Welcome back, Ro");
    assertThat(first.body().redirectAfterSeconds()).isEqualTo(2);
    assertThat(first.body().sessionHandle()).isNull();
    assertThat(first.browserSessionId()).isEqualTo("browser-session");

    JourneyBody second = journey.verifyOtp(challenge, "111111").body();
    assertThat(second.code()).isEqualTo("VAL-015");
    verify(sessions).establishSession(any(), any());
  }

  @Test
  void fiveWrongOtpsEndTheChallengeWithoutASession() {
    String challenge = issuedLoginOtp();
    for (int i = 0; i < 4; i++) {
      assertThat(journey.verifyOtp(challenge, "000000").body().code()).isEqualTo("VAL-014");
    }
    JourneyBody limit = journey.verifyOtp(challenge, "000000").body();
    assertThat(limit.code()).isEqualTo("VAL-016");
    assertThat(limit.message()).contains("Your account has not been locked.");
    verify(authorization, never()).completeLogin(any());
    verify(sessions, never()).establishSession(any(), any());
  }

  @Test
  void resendWaitsThenInvalidatesThePreviousCodeWithoutResettingAttempts() {
    String challenge = issuedLoginOtp();
    assertThat(journey.verifyOtp(challenge, "000000").body().code()).isEqualTo("VAL-014");

    JourneyBody early = journey.resend(challenge).body();
    assertThat(early.outcome()).isEqualTo("WAIT");
    assertThat(early.resendAvailableInSeconds()).isGreaterThan(0);

    clock.plus(Duration.ofMinutes(2));
    JourneyBody resent = journey.resend(challenge).body();
    assertThat(resent.outcome()).isEqualTo("OTP_SENT");
    assertThat(resent.resendsRemaining()).isEqualTo(2);

    assertThat(journey.verifyOtp(challenge, "111111").body().code()).isEqualTo("VAL-014");
    when(authorization.completeLogin(USER)).thenReturn(account("READY", 3));
    when(sessions.establishSession(any(), any()))
        .thenReturn(
            new LoginService.BankLoginResult(
                ClientType.WEB,
                true,
                USER,
                "BANK_EMPLOYEE",
                "ACTIVE",
                1,
                "browser-session",
                null,
                28800));
    assertThat(journey.verifyOtp(challenge, "222222").body().outcome()).isEqualTo("AUTHENTICATED");
  }

  @Test
  void fourthResendEndsTheJourney() {
    String challenge = issuedLoginOtp();
    clock.plus(Duration.ofMinutes(2));
    assertThat(journey.resend(challenge).body().outcome()).isEqualTo("OTP_SENT");
    clock.plus(Duration.ofMinutes(2));
    assertThat(journey.resend(challenge).body().outcome()).isEqualTo("OTP_SENT");
    clock.plus(Duration.ofMinutes(2));
    assertThat(journey.resend(challenge).body().outcome()).isEqualTo("OTP_SENT");
    clock.plus(Duration.ofMinutes(2));
    assertThat(journey.resend(challenge).body().code()).isEqualTo("VAL-017");
  }

  @Test
  void inactiveDirectoryAccountDoesNotCountAPasswordFailure() {
    when(authorization.preview("BANK_EMPLOYEE", "E100")).thenReturn(account("READY", 3));
    when(provider.verifyAd("E100", "secret"))
        .thenReturn(new IdentityProviderClient.AdVerifyResult(false, false, "E100", null, null));
    JourneyBody inactive = sign("E100");
    assertThat(inactive.code()).isEqualTo("VAL-007");
    verify(authorization, never()).recordBankPassword(any(), anyBoolean());
  }

  @Test
  void forgotPasswordWhileLockedDoesNotSendAnOtp() {
    when(authorization.assessForgotPassword("rm@aubank.in")).thenReturn(account("LOCKED", 0));
    ShownCaptcha captcha = captcha();
    JourneyBody body =
        journey
            .forgotPassword(ClientType.WEB, "rm@aubank.in", captcha.id(), captcha.value())
            .body();
    assertThat(body.title()).isEqualTo("Unlock your account first");
    assertThat(body.message()).contains("won't unlock your account");
  }

  @Test
  void alreadyActiveUnlockHasNoScreen() {
    when(authorization.assessUnlock("INSURER_REPRESENTATIVE", "rm@aubank.in"))
        .thenReturn(account("NOT_REQUIRED", 3));
    ShownCaptcha captcha = captcha();
    JourneyBody body =
        journey
            .unlock(
                new UnlockCommand(
                    ClientType.WEB,
                    WorkforceLoginUser.INSURANCE_RM,
                    "rm@aubank.in",
                    captcha.id(),
                    captcha.value()))
            .body();
    assertThat(body.outcome()).isEqualTo("NOT_REQUIRED");
    assertThat(body.message()).isNull();
  }

  @Test
  void expiredPasswordIssuesATicketAndDoesNotSendAnOtpUntilAsked() {
    when(authorization.preview("INSURER_REPRESENTATIVE", "rm@aubank.in"))
        .thenReturn(account("READY", 3));
    when(authorization.verifyPlatformPassword(USER, "Horse#99"))
        .thenReturn(new PasswordOutcome(account("PASSWORD_EXPIRED", 3), false));
    JourneyBody expired = signAs(WorkforceLoginUser.INSURANCE_RM, "rm@aubank.in", "Horse#99");
    assertThat(expired.outcome()).isEqualTo("PASSWORD_EXPIRED");
    assertThat(expired.challengeId()).isNull();
    verify(provider, never()).verifyAd(anyString(), anyString());

    JourneyBody otp = journey.sendExpiredPasswordOtp(expired.continuationToken()).body();
    assertThat(otp.outcome()).isEqualTo("OTP_SENT");
    assertThat(otp.purpose()).isEqualTo("EXPIRY");
  }

  @Test
  void passwordMismatchUsesTheDesignSentence() {
    String token = continuationAfterForgotOtp();
    when(authorization.setPassword(USER, "Zebra#99", "nope", "FORGOT"))
        .thenReturn(new IdentityAuthorizationClient.PasswordSetResult("MISMATCH", "MISMATCH"));
    JourneyBody body = journey.setPassword(token, "Zebra#99", "nope").body();
    assertThat(body.message()).isEqualTo("Both passwords must match");
    assertThat(body.outcome()).isEqualTo("MESSAGE");
  }

  @Test
  void signInCommandDoesNotPrintThePassword() {
    assertThat(signIn(WorkforceLoginUser.BANK_RM, "E100", "secret", "id", "captcha").toString())
        .doesNotContain("secret")
        .doesNotContain("captcha");
  }

  private JourneyBody sign(String loginId) {
    return signAs(WorkforceLoginUser.BANK_RM, loginId, "secret");
  }

  private JourneyBody signAs(WorkforceLoginUser userType, String loginId, String password) {
    ShownCaptcha captcha = captcha();
    return journey
        .signIn(signIn(userType, loginId, password, captcha.id(), captcha.value()))
        .body();
  }

  private ShownCaptcha captcha() {
    JourneyBody body = journey.issueCaptcha().body();
    return new ShownCaptcha(body.captchaId(), body.display());
  }

  private String issuedLoginOtp() {
    when(authorization.preview("BANK_EMPLOYEE", "E100")).thenReturn(account("READY", 3));
    when(provider.verifyAd("E100", "secret"))
        .thenReturn(new IdentityProviderClient.AdVerifyResult(true, true, "E100", "E100", "e@x"));
    when(authorization.recordBankPassword(USER, true))
        .thenReturn(new PasswordOutcome(account("READY", 3), true));
    return sign("E100").challengeId();
  }

  private String continuationAfterForgotOtp() {
    when(authorization.assessForgotPassword("rm@aubank.in")).thenReturn(account("OK", 3));
    ShownCaptcha captcha = captcha();
    JourneyBody otp =
        journey
            .forgotPassword(ClientType.WEB, "rm@aubank.in", captcha.id(), captcha.value())
            .body();
    return journey.verifyOtp(otp.challengeId(), codes.lastIssued()).body().continuationToken();
  }

  private record ShownCaptcha(String id, String value) {}

  private SignInCommand signIn(
      WorkforceLoginUser userType,
      String loginId,
      String password,
      String captchaId,
      String captchaValue) {
    return new SignInCommand(ClientType.WEB, userType, loginId, password, captchaId, captchaValue);
  }

  private static AccountView account(String route, int remaining) {
    return new AccountView(
        !"UNKNOWN".equals(route),
        route,
        "UNKNOWN".equals(route) ? null : USER,
        "ACTIVE",
        1,
        "BANK_EMPLOYEE",
        null,
        "Ro",
        "E100",
        "xx1234",
        "ro••••@aubank.in",
        remaining,
        false);
  }

  private final class SequenceCodes implements OtpCodeFactory {
    private final String[] values;
    private int index;
    private String last;

    private SequenceCodes(String... values) {
      this.values = values;
    }

    @Override
    public String nextCode() {
      last = values[Math.min(index, values.length - 1)];
      index++;
      return last;
    }

    private String lastIssued() {
      return last;
    }
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    private void plus(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
