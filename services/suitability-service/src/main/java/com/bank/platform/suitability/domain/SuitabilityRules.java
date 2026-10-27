package com.bank.platform.suitability.domain;

import com.bank.platform.suitability.domain.JourneyModel.FieldError;
import com.bank.platform.suitability.domain.JourneyModel.RawAnswers;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;

/**
 * Current-design question set for Savings Plan and ULIP. Tobacco, medical history, existing cover
 * and the investment screen are not asked. The first incomplete answer is the one Explore reports.
 */
public final class SuitabilityRules {

  private static final Set<String> POLICY_FOR =
      Set.of("Self", "Spouse", "Child", "Parents", "Someone else");
  private static final Set<String> LIFE_STAGE =
      Set.of("Single", "Married", "Married, with kids", "Pre-retirement", "Retired");
  private static final Set<String> RISK = Set.of("High certainty", "Balance", "High returns");
  private static final Set<String> GOAL =
      Set.of(
          "Protecting family",
          "Saving and growing money",
          "Investing for higher returns",
          BrdCopy.GOAL_HEALTH);
  private static final Set<String> GENDER = Set.of("Male", "Female");

  private SuitabilityRules() {}

  public static int age(LocalDate dob, LocalDate today) {
    return Period.between(dob, today).getYears();
  }

  public static String productType(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim();
    if ("SAVINGS_PLAN".equals(value) || "Savings Plan".equals(value)) {
      return "SAVINGS_PLAN";
    }
    if ("ULIP".equalsIgnoreCase(value)) {
      return "ULIP";
    }
    return null;
  }

  public static Optional<FieldError> explore(
      RawAnswers answers, JourneyModel.Masters masters, LocalDate today) {
    return validate(answers, masters, today, true);
  }

  public static Optional<FieldError> save(
      RawAnswers answers, JourneyModel.Masters masters, LocalDate today) {
    return validate(answers, masters, today, false);
  }

  private static Optional<FieldError> validate(
      RawAnswers answers, JourneyModel.Masters masters, LocalDate today, boolean required) {
    Optional<FieldError> policy =
        choice(answers.policyFor(), required, POLICY_FOR, "VAL-001", "policyFor", BrdCopy.VAL_001);
    if (policy.isPresent()) {
      return policy;
    }
    if (!BrdCopy.SELF.equals(answers.policyFor())) {
      Optional<FieldError> life =
          life(answers, today, required && !Contacts.blank(answers.policyFor()));
      if (life.isPresent()) {
        return life;
      }
    }
    Optional<FieldError> stage =
        choice(answers.lifeStage(), required, LIFE_STAGE, "VAL-009", "lifeStage", BrdCopy.VAL_009);
    if (stage.isPresent()) {
      return stage;
    }
    Optional<FieldError> risk =
        choice(answers.risk(), required, RISK, "VAL-010", "risk", BrdCopy.VAL_010);
    if (risk.isPresent()) {
      return risk;
    }
    Optional<FieldError> goal =
        choice(answers.goal(), required, GOAL, "VAL-011", "goal", BrdCopy.VAL_011);
    if (goal.isPresent()) {
      return goal;
    }
    Optional<FieldError> occupation = occupation(answers, masters, required);
    if (occupation.isPresent()) {
      return occupation;
    }
    Optional<FieldError> education = education(answers, masters, required);
    if (education.isPresent()) {
      return education;
    }
    return income(answers, masters, required);
  }

  private static Optional<FieldError> life(RawAnswers answers, LocalDate today, boolean required) {
    String name = BrdCopy.normaliseName(answers.lifeName());
    if (Contacts.blank(name)) {
      if (required) {
        return Optional.of(new FieldError("VAL-002", "lifeAssured.name", BrdCopy.VAL_002));
      }
    } else if (!BrdCopy.personName(name)) {
      return Optional.of(new FieldError("VAL-003", "lifeAssured.name", BrdCopy.VAL_003));
    }
    if (Contacts.blank(answers.lifeDob())) {
      if (required) {
        return Optional.of(new FieldError("VAL-004", "lifeAssured.dob", BrdCopy.VAL_004));
      }
    } else if (pastDate(answers.lifeDob(), today).isEmpty()) {
      return Optional.of(new FieldError("VAL-005", "lifeAssured.dob", BrdCopy.VAL_005));
    }
    if (!Contacts.blank(answers.lifeMobile()) && !Contacts.mobile(answers.lifeMobile())) {
      return Optional.of(new FieldError("VAL-006", "lifeAssured.mobile", BrdCopy.VAL_006));
    }
    if (!Contacts.blank(answers.lifeEmail()) && !Contacts.email(answers.lifeEmail())) {
      return Optional.of(new FieldError("VAL-007", "lifeAssured.email", BrdCopy.VAL_007));
    }
    if (Contacts.blank(answers.lifeGender())) {
      if (required) {
        return Optional.of(new FieldError("VAL-008", "lifeAssured.gender", BrdCopy.VAL_008));
      }
    } else if (!GENDER.contains(answers.lifeGender().trim())) {
      return Optional.of(new FieldError("VAL-008", "lifeAssured.gender", BrdCopy.VAL_008));
    }
    return Optional.empty();
  }

  private static Optional<FieldError> occupation(
      RawAnswers answers, JourneyModel.Masters masters, boolean required) {
    if (Contacts.blank(answers.occupation())) {
      if (required) {
        return Optional.of(new FieldError("VAL-012", "occupation", BrdCopy.VAL_012));
      }
      return Optional.empty();
    }
    if (!masters.occupation(answers.occupation().trim())) {
      return Optional.of(new FieldError("VAL-012", "occupation", BrdCopy.VAL_012));
    }
    if ("Others".equals(answers.occupation().trim()) && Contacts.blank(answers.occupationOther())) {
      return Optional.of(new FieldError("VAL-013", "occupationOther", BrdCopy.VAL_013));
    }
    return Optional.empty();
  }

  private static Optional<FieldError> education(
      RawAnswers answers, JourneyModel.Masters masters, boolean required) {
    if (Contacts.blank(answers.education())) {
      if (required) {
        return Optional.of(new FieldError("VAL-014", "education", BrdCopy.VAL_014));
      }
      return Optional.empty();
    }
    if (!masters.education(answers.education().trim())) {
      return Optional.of(new FieldError("VAL-014", "education", BrdCopy.VAL_014));
    }
    if ("Others".equals(answers.education().trim()) && Contacts.blank(answers.educationOther())) {
      return Optional.of(new FieldError("VAL-015", "educationOther", BrdCopy.VAL_015));
    }
    return Optional.empty();
  }

  private static Optional<FieldError> income(
      RawAnswers answers, JourneyModel.Masters masters, boolean required) {
    if (Contacts.blank(answers.incomeBand())) {
      if (required) {
        return Optional.of(new FieldError("VAL-016", "incomeBand", BrdCopy.VAL_016));
      }
      return Optional.empty();
    }
    if (!masters.incomeBand(answers.incomeBand().trim())) {
      return Optional.of(new FieldError("VAL-016", "incomeBand", BrdCopy.VAL_016));
    }
    return Optional.empty();
  }

  private static Optional<FieldError> choice(
      String value,
      boolean required,
      Set<String> allowed,
      String id,
      String field,
      String message) {
    if (Contacts.blank(value)) {
      if (required) {
        return Optional.of(new FieldError(id, field, message));
      }
      return Optional.empty();
    }
    if (!allowed.contains(value.trim())) {
      return Optional.of(new FieldError(id, field, message));
    }
    return Optional.empty();
  }

  public static Optional<LocalDate> pastDate(String raw, LocalDate today) {
    if (Contacts.blank(raw)) {
      return Optional.empty();
    }
    try {
      LocalDate date = LocalDate.parse(raw.trim());
      if (!date.isBefore(today)) {
        return Optional.empty();
      }
      return Optional.of(date);
    } catch (DateTimeException ex) {
      return Optional.empty();
    }
  }
}
