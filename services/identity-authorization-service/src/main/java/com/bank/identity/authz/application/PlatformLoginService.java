package com.bank.identity.authz.application;

import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.domain.PasswordHasher;
import com.bank.identity.authz.domain.PasswordPolicy;
import com.bank.identity.authz.domain.PasswordPolicy.Violation;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform account state for Login BRD v3.1: first login, lock, failed-password count, last
 * successful login, and the Insurance RM platform password. Captcha and OTP stay on the BFF. This
 * service never logs a password.
 */
@Service
public class PlatformLoginService {

  private final BusinessUserRepository users;
  private final PasswordHasher hasher;
  private final Clock clock;
  private final int maxPasswordFailures;
  private final Duration inactivity;
  private final Duration passwordExpiry;

  public PlatformLoginService(
      BusinessUserRepository users,
      PasswordHasher hasher,
      Clock clock,
      @Value("${identity.login.max-password-failures:3}") int maxPasswordFailures,
      @Value("${identity.login.inactivity-days:30}") int inactivityDays,
      @Value("${identity.login.password-expiry-days:60}") int passwordExpiryDays) {
    this.users = users;
    this.hasher = hasher;
    this.clock = clock;
    this.maxPasswordFailures = maxPasswordFailures;
    this.inactivity = Duration.ofDays(inactivityDays);
    this.passwordExpiry = Duration.ofDays(passwordExpiryDays);
  }

  @Transactional
  public AccountView preview(UserType userType, String loginId) {
    BusinessUserEntity user = find(userType, loginId).orElse(null);
    if (user == null) {
      return AccountView.unknown();
    }
    return view(applyInactivity(user));
  }

  @Transactional
  public PasswordOutcome recordBankPasswordResult(UUID businessUserId, boolean accepted) {
    BusinessUserEntity user = users.findById(businessUserId).orElseThrow();
    if (user.getUserType() != UserType.BANK_EMPLOYEE) {
      throw new IllegalArgumentException("Bank password result requires a bank employee");
    }
    return accepted ? accepted(user) : rejected(user);
  }

  @Transactional
  public PasswordOutcome verifyPlatformPassword(UUID businessUserId, String password) {
    BusinessUserEntity user = users.findById(businessUserId).orElseThrow();
    if (user.getUserType() != UserType.INSURER_REPRESENTATIVE) {
      throw new IllegalArgumentException("Platform password applies to an Insurance RM");
    }
    boolean accepted = hasher.matches(password, user.getPasswordHash());
    return accepted ? accepted(user) : rejected(user);
  }

  @Transactional
  public AccountView completeLogin(UUID businessUserId) {
    BusinessUserEntity user = users.findById(businessUserId).orElseThrow();
    user.recordSuccessfulPlatformLogin(clock.instant());
    return view(user);
  }

  @Transactional
  public AccountView assessUnlock(UserType userType, String loginId) {
    BusinessUserEntity user = find(userType, loginId).orElse(null);
    if (user == null) {
      return AccountView.unknown();
    }
    user = applyInactivity(user);
    if (!isActive(user) && !(user.getStatus() == UserStatus.PENDING && user.isFirstLogin())) {
      return view(user, "INACTIVE");
    }
    if (!user.isFirstLogin() && user.getLockedReason() == null) {
      return view(user, "NOT_REQUIRED");
    }
    return view(user, "UNLOCK_OK");
  }

  @Transactional
  public AccountView completeUnlock(UUID businessUserId) {
    BusinessUserEntity user = users.findById(businessUserId).orElseThrow();
    Instant now = clock.instant();
    if (!isActive(user) && user.getStatus() != UserStatus.PENDING) {
      return view(user, "INACTIVE");
    }
    if (!user.isFirstLogin() && user.getLockedReason() == null) {
      return view(user, "NOT_REQUIRED");
    }
    if (user.isFirstLogin() && user.getUserType() == UserType.INSURER_REPRESENTATIVE) {
      user.openInsurancePasswordSetup(now);
      return view(user, "SET_PASSWORD");
    }
    if (user.isFirstLogin()) {
      user.activateBankFirstLogin(now);
      return view(user, "ACTIVATED");
    }
    user.clearLockAndFailures(now);
    return view(user, "UNLOCKED");
  }

  @Transactional
  public AccountView assessForgotPassword(String email) {
    BusinessUserEntity user =
        users
            .findByEmailIgnoreCaseAndUserType(
                email == null ? "" : email.trim(), UserType.INSURER_REPRESENTATIVE)
            .orElse(null);
    if (user == null) {
      return AccountView.unknown();
    }
    user = applyInactivity(user);
    if (!isActive(user) && user.getStatus() != UserStatus.PENDING) {
      return view(user, "INACTIVE");
    }
    if (user.getLockedReason() != null) {
      return view(user, "LOCKED");
    }
    return view(user, "OK");
  }

  @Transactional
  public PasswordSetResult setPassword(
      UUID businessUserId, String newPassword, String confirmPassword, String purpose) {
    BusinessUserEntity user = users.findById(businessUserId).orElseThrow();
    if (user.getUserType() != UserType.INSURER_REPRESENTATIVE) {
      throw new IllegalArgumentException("Only an Insurance RM sets a platform password");
    }
    if ("FORGOT".equals(purpose) && user.getLockedReason() != null) {
      return new PasswordSetResult("LOCKED_UNCHANGED", Violation.OK);
    }
    boolean sameAsPrevious = hasher.matches(newPassword, user.getPasswordHash());
    Violation violation =
        PasswordPolicy.check(newPassword, confirmPassword, user.getEmail(), sameAsPrevious);
    if (violation != Violation.OK) {
      return new PasswordSetResult(violation.name(), violation);
    }
    user.replacePasswordHash(hasher.hash(newPassword), clock.instant());
    return new PasswordSetResult("SET", Violation.OK);
  }

  private PasswordOutcome accepted(BusinessUserEntity user) {
    if (user.getLockedReason() != null || user.isFirstLogin()) {
      return new PasswordOutcome(view(user), false);
    }
    if (user.getUserType() == UserType.INSURER_REPRESENTATIVE && passwordExpired(user)) {
      return new PasswordOutcome(view(user, "PASSWORD_EXPIRED"), false);
    }
    return new PasswordOutcome(view(user, "READY"), true);
  }

  private PasswordOutcome rejected(BusinessUserEntity user) {
    if (user.getLockedReason() != null) {
      return new PasswordOutcome(view(user), false);
    }
    boolean locked = user.recordPasswordFailure(maxPasswordFailures, clock.instant());
    String route = locked ? "LOCKED_PASSWORD" : "PASSWORD_REJECTED";
    return new PasswordOutcome(view(user, route), false);
  }

  private BusinessUserEntity applyInactivity(BusinessUserEntity user) {
    if (user.getLockedReason() != null || user.isFirstLogin()) {
      return user;
    }
    Instant baseline = user.getLastSuccessfulLoginAt();
    Instant cleared = user.getLockClearedAt();
    if (cleared != null && (baseline == null || cleared.isAfter(baseline))) {
      baseline = cleared;
    }
    if (baseline == null) {
      return user;
    }
    Instant now = clock.instant();
    if (!baseline.plus(inactivity).isAfter(now)) {
      user.lockForInactivity(now);
    }
    return user;
  }

  private boolean passwordExpired(BusinessUserEntity user) {
    Instant changed = user.getPasswordChangedAt();
    if (changed == null) {
      return false;
    }
    return !changed.plus(passwordExpiry).isAfter(clock.instant());
  }

  private static boolean isActive(BusinessUserEntity user) {
    return user.getStatus() == UserStatus.ACTIVE;
  }

  private java.util.Optional<BusinessUserEntity> find(UserType userType, String loginId) {
    String id = loginId == null ? "" : loginId.trim();
    if (userType == UserType.BANK_EMPLOYEE) {
      return users.findByEmployeeIdAndUserType(id, userType);
    }
    return users.findByEmailIgnoreCaseAndUserType(id, UserType.INSURER_REPRESENTATIVE);
  }

  private AccountView view(BusinessUserEntity user) {
    return view(user, routeOf(user));
  }

  private AccountView view(BusinessUserEntity user, String route) {
    int remaining = Math.max(0, maxPasswordFailures - user.getFailedPasswordCount());
    return new AccountView(
        true,
        route,
        user.getId(),
        user.getStatus().name(),
        user.getPolicyVersion(),
        user.getUserType().name(),
        user.getInsurerCode(),
        user.getFirstName(),
        user.getUsername(),
        maskMobile(user.getMobileNumber()),
        maskEmail(user.getEmail()),
        remaining,
        user.isFirstLogin());
  }

  private String routeOf(BusinessUserEntity user) {
    if (!isActive(user) && !(user.getStatus() == UserStatus.PENDING && user.isFirstLogin())) {
      return "INACTIVE";
    }
    if (user.isFirstLogin()) {
      return "FIRST_TIME";
    }
    if ("PASSWORD".equals(user.getLockedReason())) {
      return "LOCKED_PASSWORD";
    }
    if ("INACTIVITY".equals(user.getLockedReason())) {
      return "LOCKED_INACTIVITY";
    }
    return "READY";
  }

  static String maskMobile(String mobile) {
    if (mobile == null || mobile.isBlank()) {
      return "xx••••";
    }
    String digits = mobile.replaceAll("\\s", "");
    String tail = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    return "xx" + tail;
  }

  static String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return "••@••";
    }
    int at = email.indexOf('@');
    String local = email.substring(0, at);
    String domain = email.substring(at);
    String head = local.length() <= 2 ? local : local.substring(0, 2);
    return head.toLowerCase(Locale.ROOT) + "••••" + domain.toLowerCase(Locale.ROOT);
  }

  public record AccountView(
      boolean found,
      String route,
      UUID businessUserId,
      String status,
      long policyVersion,
      String userType,
      String insurerCode,
      String firstName,
      String username,
      String maskedMobile,
      String maskedEmail,
      int attemptsRemaining,
      boolean firstLogin) {

    static AccountView unknown() {
      return new AccountView(
          false, "UNKNOWN", null, null, 0, null, null, null, null, null, null, 0, false);
    }
  }

  public record PasswordOutcome(AccountView account, boolean accepted) {}

  public record PasswordSetResult(String result, Violation violation) {}
}
