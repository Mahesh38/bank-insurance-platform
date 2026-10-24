package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DevLoginAccountSeedTest {

  @Autowired DevLoginAccountSeed seed;
  @Autowired PlatformLoginService login;
  @Autowired BusinessUserRepository users;

  @Test
  void devProfileSeedsBankRmAndBothFlsCasesWithoutDuplicatingThem() {
    long before = users.count();
    seed.seed();
    assertThat(users.count()).isEqualTo(before);

    var bank = login.preview(UserType.BANK_EMPLOYEE, DevLoginAccountSeed.BANK_EMPLOYEE_ID);
    assertThat(bank.route()).isEqualTo("READY");
    assertThat(bank.firstName()).isEqualTo("Asha");

    var fls = login.preview(UserType.INSURER_REPRESENTATIVE, DevLoginAccountSeed.FLS_EMAIL);
    assertThat(fls.route()).isEqualTo("READY");
    assertThat(
            login
                .verifyPlatformPassword(fls.businessUserId(), DevLoginAccountSeed.FLS_PASSWORD)
                .accepted())
        .isTrue();
    assertThat(login.assessForgotPassword(DevLoginAccountSeed.FLS_EMAIL).route()).isEqualTo("OK");

    var first =
        login.preview(UserType.INSURER_REPRESENTATIVE, DevLoginAccountSeed.FIRST_TIME_FLS_EMAIL);
    assertThat(first.route()).isEqualTo("FIRST_TIME");
    assertThat(
            login
                .assessUnlock(
                    UserType.INSURER_REPRESENTATIVE, DevLoginAccountSeed.FIRST_TIME_FLS_EMAIL)
                .route())
        .isEqualTo("UNLOCK_OK");
  }
}
