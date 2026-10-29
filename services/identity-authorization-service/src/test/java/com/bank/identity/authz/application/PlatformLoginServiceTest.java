package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PlatformLoginServiceTest {

  @Autowired PlatformLoginService login;
  @Autowired BusinessUserRepository users;

  @Test
  void insurancePasswordFailuresLockOnTheThirdAndUnlockResetsTheCounter() {
    BusinessUserEntity partner = users.save(insurance("lock@aubank.in"));
    assertThat(login.setPassword(partner.getId(), "Horse#99", "Horse#99", "FIRST_TIME").result())
        .isEqualTo("SET");
    partner = users.findById(partner.getId()).orElseThrow();
    assertThat(partner.isFirstLogin()).isFalse();
    assertThat(partner.getStatus().name()).isEqualTo("ACTIVE");

    assertThat(login.verifyPlatformPassword(partner.getId(), "wrong").account().route())
        .isEqualTo("PASSWORD_REJECTED");
    assertThat(login.verifyPlatformPassword(partner.getId(), "wrong").account().attemptsRemaining())
        .isEqualTo(1);
    var third = login.verifyPlatformPassword(partner.getId(), "wrong");
    assertThat(third.account().route()).isEqualTo("LOCKED_PASSWORD");
    assertThat(third.account().attemptsRemaining()).isZero();

    assertThat(login.assessForgotPassword("lock@aubank.in").route()).isEqualTo("LOCKED");
    assertThat(login.setPassword(partner.getId(), "Zebra#99", "Zebra#99", "FORGOT").result())
        .isEqualTo("LOCKED_UNCHANGED");
    assertThat(login.verifyPlatformPassword(partner.getId(), "Horse#99").account().route())
        .isEqualTo("LOCKED_PASSWORD");

    assertThat(login.completeUnlock(partner.getId()).route()).isEqualTo("UNLOCKED");
    assertThat(login.verifyPlatformPassword(partner.getId(), "Horse#99").accepted()).isTrue();
  }

  @Test
  void keycloakRejectionLocksAnInsuranceUserWithoutReadingTheLocalHash() {
    BusinessUserEntity partner = users.save(insurance("kc@aubank.in"));
    assertThat(login.setPassword(partner.getId(), "Horse#99", "Horse#99", "FIRST_TIME").result())
        .isEqualTo("SET");
    assertThat(login.recordInsurancePasswordResult(partner.getId(), true).accepted()).isTrue();
    assertThat(login.recordInsurancePasswordResult(partner.getId(), false).account().route())
        .isEqualTo("PASSWORD_REJECTED");
    assertThat(
            login
                .recordInsurancePasswordResult(partner.getId(), false)
                .account()
                .attemptsRemaining())
        .isEqualTo(1);
    assertThat(login.recordInsurancePasswordResult(partner.getId(), false).account().route())
        .isEqualTo("LOCKED_PASSWORD");
    assertThat(login.recordInsurancePasswordResult(partner.getId(), true).accepted()).isFalse();
    int failures = users.findById(partner.getId()).orElseThrow().getFailedPasswordCount();
    login.recordInsurancePasswordResult(partner.getId(), false);
    assertThat(users.findById(partner.getId()).orElseThrow().getFailedPasswordCount())
        .isEqualTo(failures);
  }

  @Test
  void passwordResultRejectsTheOtherPopulationAndAFirstLoginSuccessStaysClosed() {
    BusinessUserEntity bank =
        users.save(
            BusinessUserEntity.bankEmployee("BANK_AD", "e-kc", "E300", "e300@aubank.in", "E300"));
    assertThatThrownBy(() -> login.recordInsurancePasswordResult(bank.getId(), true))
        .isInstanceOf(IllegalArgumentException.class);
    BusinessUserEntity partner = users.save(insurance("fresh@aubank.in"));
    assertThatThrownBy(() -> login.recordBankPasswordResult(partner.getId(), true))
        .isInstanceOf(IllegalArgumentException.class);

    var first = login.recordInsurancePasswordResult(partner.getId(), true);
    assertThat(first.accepted()).isFalse();
    assertThat(first.account().route()).isEqualTo("FIRST_TIME");
  }

  @Test
  void firstTimeBankActivatesWithoutAPasswordChangeAndInsuranceMustSetOne() {
    BusinessUserEntity bank =
        users.save(
            BusinessUserEntity.bankEmployee("BANK_AD", "e-1", "E100", "e100@aubank.in", "E100"));
    bank.markFirstLoginForProvisioning();
    users.save(bank);
    assertThat(login.preview(UserType.BANK_EMPLOYEE, "E100").route()).isEqualTo("FIRST_TIME");
    assertThat(login.completeUnlock(bank.getId()).route()).isEqualTo("ACTIVATED");
    assertThat(users.findById(bank.getId()).orElseThrow().isFirstLogin()).isFalse();
    assertThat(users.findById(bank.getId()).orElseThrow().getPasswordHash()).isNull();

    BusinessUserEntity partner = users.save(insurance("first@aubank.in"));
    assertThat(login.preview(UserType.INSURER_REPRESENTATIVE, "first@aubank.in").route())
        .isEqualTo("FIRST_TIME");
    assertThat(login.assessUnlock(UserType.INSURER_REPRESENTATIVE, "first@aubank.in").route())
        .isEqualTo("UNLOCK_OK");
    assertThat(login.completeUnlock(partner.getId()).route()).isEqualTo("SET_PASSWORD");
    assertThat(users.findById(partner.getId()).orElseThrow().isFirstLogin()).isTrue();
    assertThat(users.findById(partner.getId()).orElseThrow().getStatus().name())
        .isEqualTo("ACTIVE");
  }

  @Test
  void alreadyActiveUnlockChangesNothing() {
    BusinessUserEntity bank =
        users.save(
            BusinessUserEntity.bankEmployee("BANK_AD", "e-2", "E200", "e200@aubank.in", "E200"));
    assertThat(login.assessUnlock(UserType.BANK_EMPLOYEE, "E200").route())
        .isEqualTo("NOT_REQUIRED");
    assertThat(login.completeUnlock(bank.getId()).route()).isEqualTo("NOT_REQUIRED");
  }

  @Test
  void inactivityLocksAndSuccessfulLoginResetsItOnlyAfterComplete() {
    BusinessUserEntity partner = users.save(insurance("idle@aubank.in"));
    login.setPassword(partner.getId(), "Horse#99", "Horse#99", "FIRST_TIME");
    partner = users.findById(partner.getId()).orElseThrow();
    partner.recordSuccessfulPlatformLogin(Instant.now().minus(31, ChronoUnit.DAYS));
    users.save(partner);

    assertThat(login.preview(UserType.INSURER_REPRESENTATIVE, "idle@aubank.in").route())
        .isEqualTo("LOCKED_INACTIVITY");
    login.completeUnlock(partner.getId());
    assertThat(login.preview(UserType.INSURER_REPRESENTATIVE, "idle@aubank.in").route())
        .isEqualTo("READY");
    assertThat(users.findById(partner.getId()).orElseThrow().getLastSuccessfulLoginAt())
        .isBefore(Instant.now().minus(30, ChronoUnit.DAYS));
  }

  @Test
  void expiredPasswordDoesNotCountAsAFailureAndMustDifferFromThePrevious() {
    BusinessUserEntity partner = users.save(insurance("old@aubank.in"));
    login.setPassword(partner.getId(), "Horse#99", "Horse#99", "FIRST_TIME");
    partner = users.findById(partner.getId()).orElseThrow();
    partner.replacePasswordHash(
        partner.getPasswordHash(), Instant.now().minus(61, ChronoUnit.DAYS));
    users.save(partner);

    var expired = login.verifyPlatformPassword(partner.getId(), "Horse#99");
    assertThat(expired.account().route()).isEqualTo("PASSWORD_EXPIRED");
    assertThat(expired.accepted()).isFalse();
    assertThat(expired.account().attemptsRemaining()).isEqualTo(3);

    assertThat(login.setPassword(partner.getId(), "Horse#99", "Horse#99", "EXPIRY").result())
        .isEqualTo("PREVIOUS");
    assertThat(login.setPassword(partner.getId(), "Zebra#99", "nope", "EXPIRY").result())
        .isEqualTo("MISMATCH");
    assertThat(login.setPassword(partner.getId(), "Zebra#99", "Zebra#99", "EXPIRY").result())
        .isEqualTo("SET");
  }

  @Test
  void unknownInsuranceEmailAndInactiveUserStop() {
    assertThat(login.preview(UserType.INSURER_REPRESENTATIVE, "missing@aubank.in").route())
        .isEqualTo("UNKNOWN");
    assertThat(PlatformLoginService.maskMobile("9876543210")).isEqualTo("xx3210");
    assertThat(PlatformLoginService.maskEmail("Ro.Hit@aubank.in")).isEqualTo("ro••••@aubank.in");
    BusinessUserEntity partner = users.save(insurance("gone@aubank.in"));
    partner.markProvisioned("subject-gone");
    partner.suspend();
    users.save(partner);
    assertThat(login.preview(UserType.INSURER_REPRESENTATIVE, "gone@aubank.in").route())
        .isEqualTo("INACTIVE");
    assertThat(login.assessForgotPassword("gone@aubank.in").route()).isEqualTo("INACTIVE");
  }

  private static BusinessUserEntity insurance(String email) {
    return BusinessUserEntity.pendingPartner("rm." + email, email, "Ro", "Hit", null);
  }
}
