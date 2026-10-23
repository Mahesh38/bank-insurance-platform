package com.bank.workforce.bff.application.login;

import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.application.LoginService;
import com.bank.workforce.bff.application.login.InMemoryChallengeStore.Continuation;
import com.bank.workforce.bff.application.login.InMemoryChallengeStore.HeldAccount;
import com.bank.workforce.bff.application.login.InMemoryChallengeStore.OtpChallenge;
import com.bank.workforce.bff.application.login.LoginMessages.Text;
import com.bank.workforce.bff.client.IdentityAuthorizationClient;
import com.bank.workforce.bff.client.IdentityAuthorizationClient.AccountView;
import com.bank.workforce.bff.client.IdentityAuthorizationClient.PasswordOutcome;
import com.bank.workforce.bff.client.IdentityAuthorizationClient.PasswordSetResult;
import com.bank.workforce.bff.client.IdentityProviderClient;
import com.bank.workforce.bff.config.LoginJourneyProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Login BRD v3.1 journey on the BFF. A workforce session is created only after a successful login
 * OTP. Unlock, forgot-password and expiry do not log the user in. Passwords and OTP codes are not
 * logged.
 */
@Service
public class LoginJourneyService {

  private static final Logger log = LoggerFactory.getLogger(LoginJourneyService.class);
  private static final String CAPTCHA_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

  private final IdentityAuthorizationClient authorization;
  private final IdentityProviderClient provider;
  private final LoginService sessions;
  private final InMemoryChallengeStore challenges;
  private final OtpCodeFactory otpCodes;
  private final LoginJourneyProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public LoginJourneyService(
      IdentityAuthorizationClient authorization,
      IdentityProviderClient provider,
      LoginService sessions,
      InMemoryChallengeStore challenges,
      OtpCodeFactory otpCodes,
      LoginJourneyProperties properties,
      Clock clock) {
    this.authorization = authorization;
    this.provider = provider;
    this.sessions = sessions;
    this.challenges = challenges;
    this.otpCodes = otpCodes;
    this.properties = properties;
    this.clock = clock;
  }

  public JourneyResult issueCaptcha() {
    String display = captchaText();
    String id = token();
    String salt = OtpDigest.newSalt();
    challenges.putCaptcha(
        id,
        salt + "$" + OtpDigest.hash(salt, display),
        clock.instant().plus(properties.captchaTtl()));
    return JourneyResult.body(
        JourneyBody.captcha(id, display, (int) properties.captchaTtl().toSeconds()));
  }

  public JourneyResult signIn(SignInCommand command) {
    return guard(
        () -> {
          JourneyResult captcha = consumeCaptcha(command.captchaId(), command.captchaValue());
          if (captcha != null) {
            return captcha;
          }
          AccountView account =
              authorization.preview(command.userType().platformType(), command.loginId().trim());
          if (account == null) {
            return unavailable();
          }
          if (!"READY".equals(account.route())) {
            log.info("Sign-in routed before the password check: {}", account.route());
            return message(
                LoginMessages.forPreview(
                    command.userType() == WorkforceLoginUser.BANK_RM, account.route()));
          }
          if (command.userType() == WorkforceLoginUser.BANK_RM) {
            return bankPassword(command, account);
          }
          return insurancePassword(command, account);
        });
  }

  public JourneyResult verifyOtp(String challengeId, String otp) {
    return guard(
        () -> {
          var verdict =
              challenges.submitOtp(challengeId, otp, clock.instant(), properties.maxOtpAttempts());
          return switch (verdict.check()) {
            case MISSING, EXPIRED -> message(LoginMessages.expiredOtp());
            case REJECTED -> message(LoginMessages.incorrectOtp());
            case LOCKED_OUT -> message(LoginMessages.otpAttemptLimit());
            case ACCEPTED -> finishOtp(verdict.challenge());
          };
        });
  }

  public JourneyResult resend(String challengeId) {
    return guard(
        () -> {
          Instant now = clock.instant();
          var verdict =
              challenges.resend(
                  challengeId,
                  otpCodes::nextCode,
                  now,
                  now.plus(properties.otpTtl()),
                  now.plus(properties.resendDelay()),
                  properties.maxResends());
          return switch (verdict.check()) {
            case MISSING -> message(LoginMessages.expiredOtp());
            case LIMIT -> message(LoginMessages.resendLimit());
            case TOO_SOON -> JourneyResult.body(wait(verdict.challenge(), now));
            case SENT -> JourneyResult.body(otpSent(verdict.challenge(), now));
          };
        });
  }

  public JourneyResult unlock(UnlockCommand command) {
    return guard(
        () -> {
          JourneyResult captcha = consumeCaptcha(command.captchaId(), command.captchaValue());
          if (captcha != null) {
            return captcha;
          }
          AccountView account =
              authorization.assessUnlock(
                  command.userType().platformType(), command.loginId().trim());
          if (account == null) {
            return unavailable();
          }
          return switch (account.route() == null ? "" : account.route()) {
            case "NOT_REQUIRED" -> JourneyResult.body(JourneyBody.notRequired());
            case "UNLOCK_OK" -> beginOtp("UNLOCK", command.clientType(), account);
            default ->
                message(
                    LoginMessages.forPreview(
                        command.userType() == WorkforceLoginUser.BANK_RM, account.route()));
          };
        });
  }

  public JourneyResult forgotPassword(
      ClientType clientType, String email, String captchaId, String captchaValue) {
    return guard(
        () -> {
          JourneyResult captcha = consumeCaptcha(captchaId, captchaValue);
          if (captcha != null) {
            return captcha;
          }
          AccountView account = authorization.assessForgotPassword(email.trim());
          if (account == null) {
            return unavailable();
          }
          return switch (account.route() == null ? "" : account.route()) {
            case "OK" -> beginOtp("FORGOT", clientType, account);
            case "UNKNOWN" -> message(LoginMessages.forgotUnknown());
            case "INACTIVE" -> message(LoginMessages.forgotInactive());
            case "LOCKED" -> message(LoginMessages.forgotLocked());
            default -> unavailable();
          };
        });
  }

  public JourneyResult sendExpiredPasswordOtp(String expiryTicket) {
    return guard(
        () -> {
          Continuation ticket = takeLive(expiryTicket);
          if (ticket == null || !"EXPIRY_TICKET".equals(ticket.purpose())) {
            return unavailable();
          }
          return beginOtp("EXPIRY", ticket.clientType(), ticket.account());
        });
  }

  public JourneyResult setPassword(String resetToken, String newPassword, String confirmPassword) {
    return guard(
        () -> {
          Continuation ticket = takeLive(resetToken);
          if (ticket == null || "EXPIRY_TICKET".equals(ticket.purpose())) {
            return unavailable();
          }
          PasswordSetResult result =
              authorization.setPassword(
                  ticket.account().businessUserId(),
                  newPassword,
                  confirmPassword,
                  ticket.purpose());
          if (result == null) {
            return unavailable();
          }
          return switch (result.result()) {
            case "SET" -> JourneyResult.body(passwordSet(ticket.purpose()));
            case "LOCKED_UNCHANGED" -> message(LoginMessages.forgotLocked());
            case "MISMATCH" -> message(LoginMessages.passwordMismatch());
            case "PREVIOUS" -> message(LoginMessages.previousPassword());
            default -> message(LoginMessages.checklist(result.violation()));
          };
        });
  }

  private JourneyResult bankPassword(SignInCommand command, AccountView account) {
    var ad = provider.verifyAd(command.loginId().trim(), command.password());
    if (ad == null) {
      return unavailable();
    }
    if (!ad.accountActive()) {
      log.info("Directory account is inactive; password attempts were not counted");
      return message(LoginMessages.inactiveLogin());
    }
    PasswordOutcome outcome =
        authorization.recordBankPassword(account.businessUserId(), ad.authenticated());
    return afterPassword(command.clientType(), outcome);
  }

  private JourneyResult insurancePassword(SignInCommand command, AccountView account) {
    PasswordOutcome outcome =
        authorization.verifyPlatformPassword(account.businessUserId(), command.password());
    if (outcome != null
        && outcome.account() != null
        && "PASSWORD_EXPIRED".equals(outcome.account().route())) {
      log.info("Platform password is expired; attempts were not counted");
      return expiryTicket(command.clientType(), outcome.account());
    }
    return afterPassword(command.clientType(), outcome);
  }

  private JourneyResult afterPassword(ClientType clientType, PasswordOutcome outcome) {
    if (outcome == null || outcome.account() == null) {
      return unavailable();
    }
    if (outcome.accepted()) {
      return beginOtp("LOGIN", clientType, outcome.account());
    }
    log.info("Password rejected with route {}", outcome.account().route());
    return message(
        LoginMessages.forPasswordFailure(
            outcome.account().route(), outcome.account().attemptsRemaining()));
  }

  private JourneyResult finishOtp(OtpChallenge challenge) {
    return switch (challenge.purpose()) {
      case "LOGIN" -> completeLogin(challenge);
      case "UNLOCK" -> completeUnlock(challenge);
      case "FORGOT" -> continuation("FORGOT", challenge.clientType(), challenge.account());
      case "EXPIRY" -> continuation("EXPIRY", challenge.clientType(), challenge.account());
      default -> unavailable();
    };
  }

  private JourneyResult completeLogin(OtpChallenge challenge) {
    AccountView done = authorization.completeLogin(challenge.account().businessUserId());
    if (done == null || !"ACTIVE".equals(done.status())) {
      return message(LoginMessages.inactiveLogin());
    }
    var session =
        sessions.establishSession(
            challenge.clientType(),
            new LoginService.EstablishedIdentity(
                done.businessUserId(),
                done.userType(),
                done.status(),
                done.policyVersion(),
                done.insurerCode(),
                done.username()));
    String name = hasText(done.firstName()) ? done.firstName() : done.username();
    String title = hasText(name) ? "Welcome back, " + name : "Welcome back";
    JourneyBody body =
        new JourneyBody(
            "AUTHENTICATED",
            null,
            title,
            "You're signed in. Taking you to your dashboard.",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            2,
            true,
            done.businessUserId(),
            done.userType(),
            done.status(),
            done.policyVersion(),
            session.nativeSessionHandle(),
            null,
            null);
    return new JourneyResult(body, session.browserSessionId());
  }

  private JourneyResult completeUnlock(OtpChallenge challenge) {
    AccountView done = authorization.completeUnlock(challenge.account().businessUserId());
    if (done == null) {
      return unavailable();
    }
    boolean bank = "BANK_EMPLOYEE".equals(done.userType());
    return switch (done.route()) {
      case "ACTIVATED" ->
          JourneyResult.body(
              success(
                  "COMPLETED",
                  "Account activated",
                  "Sign in with your employee ID and your existing network password.",
                  3));
      case "UNLOCKED" ->
          JourneyResult.body(
              success(
                  "COMPLETED",
                  "Account unlocked",
                  bank
                      ? "Sign in with your employee ID and your existing network password."
                      : "Sign in with your corporate email ID and your existing password.",
                  3));
      case "SET_PASSWORD" -> continuation("FIRST_TIME", challenge.clientType(), held(done));
      case "NOT_REQUIRED" -> JourneyResult.body(JourneyBody.notRequired());
      case "INACTIVE" -> message(LoginMessages.inactiveLogin());
      default -> unavailable();
    };
  }

  private JourneyResult beginOtp(String purpose, ClientType clientType, AccountView account) {
    return beginOtp(purpose, clientType, held(account));
  }

  private JourneyResult beginOtp(String purpose, ClientType clientType, HeldAccount account) {
    Instant now = clock.instant();
    String salt = OtpDigest.newSalt();
    String code = otpCodes.nextCode();
    OtpChallenge challenge =
        new OtpChallenge(
            token(),
            purpose,
            clientType,
            account,
            salt,
            OtpDigest.hash(salt, code),
            now.plus(properties.otpTtl()),
            now.plus(properties.resendDelay()),
            0,
            0);
    challenges.putOtp(challenge);
    log.info("OTP issued for purpose {}", purpose);
    return JourneyResult.body(otpSent(challenge, now));
  }

  private JourneyResult expiryTicket(ClientType clientType, AccountView account) {
    Continuation ticket =
        new Continuation(
            token(),
            "EXPIRY_TICKET",
            clientType,
            held(account),
            clock.instant().plus(properties.continuationTtl()));
    challenges.putContinuation(ticket);
    return JourneyResult.body(
        new JourneyBody(
            "PASSWORD_EXPIRED",
            null,
            "Your password has expired",
            "Insurance Hub passwords expire every 60 days. Verify it's you with an OTP to set a new password.",
            null,
            null,
            null,
            null,
            ticket.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            account.businessUserId(),
            account.userType(),
            account.status(),
            account.policyVersion(),
            null,
            "EXPIRY",
            null));
  }

  private JourneyResult continuation(String purpose, ClientType clientType, HeldAccount account) {
    Continuation ticket =
        new Continuation(
            token(),
            purpose,
            clientType,
            account,
            clock.instant().plus(properties.continuationTtl()));
    challenges.putContinuation(ticket);
    return JourneyResult.body(
        new JourneyBody(
            "CONTINUATION",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ticket.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            account.businessUserId(),
            account.userType(),
            account.status(),
            account.policyVersion(),
            null,
            purpose,
            null));
  }

  private JourneyBody passwordSet(String purpose) {
    return switch (purpose) {
      case "FIRST_TIME" ->
          success(
              "PASSWORD_SET",
              "Account unlocked",
              "Your new password is set. Sign in with your corporate email ID and new password.",
              3);
      case "EXPIRY" ->
          success(
              "PASSWORD_SET",
              "Password updated",
              "Your new password is valid for the next 60 days. Sign in again to continue.",
              3);
      default ->
          success(
              "PASSWORD_SET",
              "Password changed",
              "Your password has been updated. Sign in again with your new password.",
              3);
    };
  }

  private static JourneyBody success(
      String outcome, String title, String message, int redirectAfterSeconds) {
    return new JourneyBody(
        outcome,
        null,
        title,
        message,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        redirectAfterSeconds,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private JourneyBody otpSent(OtpChallenge challenge, Instant now) {
    return new JourneyBody(
        "OTP_SENT",
        null,
        "Verify it's you",
        null,
        null,
        null,
        null,
        challenge.id(),
        null,
        challenge.account().maskedMobile(),
        challenge.account().maskedEmail(),
        secondsUntil(now, challenge.expiresAt()),
        secondsUntil(now, challenge.resendNotBefore()),
        Math.max(0, properties.maxResends() - challenge.resendsUsed()),
        null,
        null,
        challenge.account().businessUserId(),
        challenge.account().userType(),
        null,
        null,
        null,
        challenge.purpose(),
        null);
  }

  private JourneyBody wait(OtpChallenge challenge, Instant now) {
    return new JourneyBody(
        "WAIT",
        null,
        null,
        null,
        null,
        null,
        null,
        challenge.id(),
        null,
        challenge.account().maskedMobile(),
        challenge.account().maskedEmail(),
        secondsUntil(now, challenge.expiresAt()),
        secondsUntil(now, challenge.resendNotBefore()),
        Math.max(0, properties.maxResends() - challenge.resendsUsed()),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        challenge.purpose(),
        null);
  }

  private JourneyResult consumeCaptcha(String id, String value) {
    var taken = challenges.takeCaptcha(id);
    if (taken.isEmpty() || clock.instant().isAfter(taken.get().expiresAt())) {
      return message(LoginMessages.wrongCaptcha());
    }
    String stored = taken.get().hash();
    int split = stored.indexOf('$');
    if (split < 0) {
      return message(LoginMessages.wrongCaptcha());
    }
    String salt = stored.substring(0, split);
    String hash = stored.substring(split + 1);
    if (!OtpDigest.matches(value, salt, hash)) {
      return message(LoginMessages.wrongCaptcha());
    }
    return null;
  }

  private Continuation takeLive(String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    Continuation ticket = challenges.takeContinuation(id).orElse(null);
    if (ticket == null || clock.instant().isAfter(ticket.expiresAt())) {
      return null;
    }
    return ticket;
  }

  private static HeldAccount held(AccountView account) {
    return new HeldAccount(
        account.businessUserId(),
        account.userType(),
        account.status(),
        account.policyVersion(),
        account.insurerCode(),
        account.firstName(),
        account.username(),
        account.maskedMobile(),
        account.maskedEmail());
  }

  private JourneyResult guard(Supplier<JourneyResult> call) {
    try {
      return call.get();
    } catch (ServiceException | RestClientException ex) {
      log.warn("Login journey downstream call failed");
      return unavailable();
    }
  }

  private static JourneyResult message(Text text) {
    return JourneyResult.body(JourneyBody.message(text));
  }

  private static JourneyResult unavailable() {
    return message(LoginMessages.unavailable());
  }

  private String captchaText() {
    StringBuilder text = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      text.append(CAPTCHA_ALPHABET.charAt(random.nextInt(CAPTCHA_ALPHABET.length())));
    }
    return text.toString();
  }

  private String token() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static int secondsUntil(Instant now, Instant then) {
    long seconds = Duration.between(now, then).toSeconds();
    return (int) Math.max(0, seconds);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public enum WorkforceLoginUser {
    BANK_RM,
    INSURANCE_RM;

    public String platformType() {
      return this == BANK_RM ? "BANK_EMPLOYEE" : "INSURER_REPRESENTATIVE";
    }
  }

  public record SignInCommand(
      ClientType clientType,
      WorkforceLoginUser userType,
      String loginId,
      String password,
      String captchaId,
      String captchaValue) {
    @Override
    public String toString() {
      return "SignInCommand[clientType=" + clientType + ", userType=" + userType + "]";
    }
  }

  public record UnlockCommand(
      ClientType clientType,
      WorkforceLoginUser userType,
      String loginId,
      String captchaId,
      String captchaValue) {
    @Override
    public String toString() {
      return "UnlockCommand[clientType=" + clientType + ", userType=" + userType + "]";
    }
  }

  public record JourneyResult(JourneyBody body, String browserSessionId) {
    static JourneyResult body(JourneyBody body) {
      return new JourneyResult(body, null);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record JourneyBody(
      String outcome,
      String code,
      String title,
      String message,
      String field,
      String captchaId,
      String display,
      String challengeId,
      String continuationToken,
      String maskedMobile,
      String maskedEmail,
      Integer expiresInSeconds,
      Integer resendAvailableInSeconds,
      Integer resendsRemaining,
      Integer redirectAfterSeconds,
      Boolean authenticated,
      UUID businessUserId,
      String userType,
      String status,
      Long policyVersion,
      String sessionHandle,
      String purpose,
      Integer attemptsRemaining) {

    static JourneyBody message(Text text) {
      return new JourneyBody(
          "MESSAGE",
          text.code(),
          text.title(),
          text.message(),
          text.field(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    static JourneyBody captcha(String id, String display, int expiresInSeconds) {
      return new JourneyBody(
          "CAPTCHA",
          null,
          null,
          null,
          null,
          id,
          display,
          null,
          null,
          null,
          null,
          expiresInSeconds,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    static JourneyBody notRequired() {
      return new JourneyBody(
          "NOT_REQUIRED",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }
  }
}
