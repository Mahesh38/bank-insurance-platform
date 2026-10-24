package com.bank.identity.authz.application;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.domain.PasswordHasher;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Clock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dev-profile accounts so the Login BRD v3.1 journey can be exercised without a bank directory or
 * an SMS gateway. Insurance RM and FLS are the same login ({@code INSURANCE_RM}). Refused under
 * prod and uat. Passwords are not logged.
 */
@Component
@Profile("dev")
public class DevLoginAccountSeed implements ApplicationRunner {

  public static final String BANK_EMPLOYEE_ID = "EMP001";
  public static final String FLS_EMAIL = "fls.rm@example.test";
  public static final String FLS_PASSWORD = "Horse#99";
  public static final String FIRST_TIME_FLS_EMAIL = "fls.new@example.test";

  private final BusinessUserRepository users;
  private final PasswordHasher hasher;
  private final Clock clock;
  private final Environment environment;
  private final TransactionTemplate transactions;

  public DevLoginAccountSeed(
      BusinessUserRepository users,
      PasswordHasher hasher,
      Clock clock,
      Environment environment,
      PlatformTransactionManager transactions) {
    this.users = users;
    this.hasher = hasher;
    this.clock = clock;
    this.environment = environment;
    this.transactions = new TransactionTemplate(transactions);
  }

  @Override
  public void run(ApplicationArguments args) {
    transactions.executeWithoutResult(status -> seed());
  }

  public void seed() {
    if (environment.matchesProfiles("prod", "uat")) {
      throw new IllegalStateException("Dev login accounts are refused under prod or uat");
    }
    if (users.findByEmployeeIdAndUserType(BANK_EMPLOYEE_ID, UserType.BANK_EMPLOYEE).isEmpty()) {
      BusinessUserEntity bank =
          BusinessUserEntity.bankEmployee(
              "BANK_AD",
              BANK_EMPLOYEE_ID,
              BANK_EMPLOYEE_ID,
              "emp001@example.test",
              BANK_EMPLOYEE_ID);
      bank.assignName("Asha", "Kulkarni");
      bank.assignMobileNumber("919800001234");
      users.save(bank);
    }
    if (users
        .findByEmailIgnoreCaseAndUserType(FLS_EMAIL, UserType.INSURER_REPRESENTATIVE)
        .isEmpty()) {
      BusinessUserEntity fls =
          BusinessUserEntity.pendingPartner("fls.rm", FLS_EMAIL, "Meera", "Shah", null);
      fls.assignProviderSubject(FLS_EMAIL);
      fls.assignMobileNumber("919800001235");
      fls.replacePasswordHash(hasher.hash(FLS_PASSWORD), clock.instant());
      users.save(fls);
    }
    if (users
        .findByEmailIgnoreCaseAndUserType(FIRST_TIME_FLS_EMAIL, UserType.INSURER_REPRESENTATIVE)
        .isEmpty()) {
      BusinessUserEntity first =
          BusinessUserEntity.pendingPartner("fls.new", FIRST_TIME_FLS_EMAIL, "Dev", "Fls", null);
      first.assignProviderSubject(FIRST_TIME_FLS_EMAIL);
      first.assignMobileNumber("919800001236");
      users.save(first);
    }
  }
}
