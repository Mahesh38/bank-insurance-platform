package com.bank.platform.suitability.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.suitability.domain.JourneyModel.FieldError;
import com.bank.platform.suitability.domain.JourneyModel.IncomeBand;
import com.bank.platform.suitability.domain.JourneyModel.Masters;
import com.bank.platform.suitability.domain.JourneyModel.RawAnswers;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SuitabilityRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
  private static final Masters MASTERS = new FixedMasters();

  @Test
  void selfProfileIsCompleteWithoutLifeAssured() {
    assertThat(SuitabilityRules.explore(self(), MASTERS, TODAY)).isEmpty();
  }

  @Test
  void missingPolicyForIsVal001() {
    RawAnswers answers = copy(self(), "", false, "", "", "", "", "");
    assertThat(id(SuitabilityRules.explore(answers, MASTERS, TODAY))).isEqualTo("VAL-001");
  }

  @Test
  void nonSelfMissingNameIsVal002() {
    RawAnswers answers = copy(self(), "Spouse", true, "", "2010-05-01", "Female", "", "");
    assertThat(message(SuitabilityRules.explore(answers, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_002);
  }

  @Test
  void invalidNameIsVal003() {
    RawAnswers answers = copy(self(), "Child", true, "Mary-Ann", "2010-05-01", "Female", "", "");
    assertThat(message(SuitabilityRules.explore(answers, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_003);
  }

  @Test
  void missingDobIsVal004AndFutureDobIsVal005() {
    RawAnswers missing = copy(self(), "Child", true, "Meera Rao", "", "Female", "", "");
    assertThat(message(SuitabilityRules.explore(missing, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_004);
    RawAnswers future = copy(self(), "Child", true, "Meera Rao", "2026-10-27", "Female", "", "");
    assertThat(message(SuitabilityRules.explore(future, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_005);
  }

  @Test
  void invalidMobileAndEmailAndGender() {
    RawAnswers mobile =
        copy(self(), "Child", true, "Meera Rao", "2010-05-01", "Female", "12345", "");
    assertThat(message(SuitabilityRules.explore(mobile, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_006);
    RawAnswers email =
        copy(self(), "Child", true, "Meera Rao", "2010-05-01", "Female", "", "not-an-email");
    assertThat(message(SuitabilityRules.explore(email, MASTERS, TODAY))).isEqualTo(BrdCopy.VAL_007);
    RawAnswers gender = copy(self(), "Child", true, "Meera Rao", "2010-05-01", "", "", "");
    assertThat(message(SuitabilityRules.explore(gender, MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_008);
  }

  @Test
  void missingLifeStageRiskAndGoal() {
    assertThat(message(SuitabilityRules.explore(copyStage(self(), ""), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_009);
    assertThat(message(SuitabilityRules.explore(copyRisk(self(), ""), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_010);
    assertThat(message(SuitabilityRules.explore(copyGoal(self(), ""), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_011);
  }

  @Test
  void occupationEducationAndIncome() {
    assertThat(
            message(SuitabilityRules.explore(copyWork(self(), "", "", "", "", ""), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_012);
    assertThat(
            message(
                SuitabilityRules.explore(
                    copyWork(self(), "Others", "", "Graduate", "", BrdCopy.BAND_5_10),
                    MASTERS,
                    TODAY)))
        .isEqualTo(BrdCopy.VAL_013);
    assertThat(
            message(
                SuitabilityRules.explore(
                    copyWork(self(), "Salaried", "", "", "", BrdCopy.BAND_5_10), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_014);
    assertThat(
            message(
                SuitabilityRules.explore(
                    copyWork(self(), "Salaried", "", "Others", "", BrdCopy.BAND_5_10),
                    MASTERS,
                    TODAY)))
        .isEqualTo(BrdCopy.VAL_015);
    assertThat(
            message(
                SuitabilityRules.explore(
                    copyWork(self(), "Salaried", "", "Graduate", "", "unknown"), MASTERS, TODAY)))
        .isEqualTo(BrdCopy.VAL_016);
  }

  @Test
  void partialSaveAllowsABlankLaterScreen() {
    RawAnswers answers = copyStage(self(), "");
    assertThat(SuitabilityRules.save(answers, MASTERS, TODAY)).isEmpty();
  }

  private static String id(Optional<FieldError> error) {
    return error.orElseThrow().id();
  }

  private static String message(Optional<FieldError> error) {
    return error.orElseThrow().message();
  }

  private static RawAnswers self() {
    return new RawAnswers(
        "Self",
        false,
        "",
        "",
        "",
        "",
        "",
        "Married",
        "Balance",
        "Saving and growing money",
        "Salaried",
        "",
        "Graduate",
        "",
        BrdCopy.BAND_5_10);
  }

  private static RawAnswers copy(
      RawAnswers base,
      String policyFor,
      boolean lifePresent,
      String name,
      String dob,
      String gender,
      String mobile,
      String email) {
    return new RawAnswers(
        policyFor,
        lifePresent,
        name,
        dob,
        gender,
        mobile,
        email,
        base.lifeStage(),
        base.risk(),
        base.goal(),
        base.occupation(),
        base.occupationOther(),
        base.education(),
        base.educationOther(),
        base.incomeBand());
  }

  private static RawAnswers copyStage(RawAnswers base, String lifeStage) {
    return new RawAnswers(
        base.policyFor(),
        base.lifePresent(),
        base.lifeName(),
        base.lifeDob(),
        base.lifeGender(),
        base.lifeMobile(),
        base.lifeEmail(),
        lifeStage,
        base.risk(),
        base.goal(),
        base.occupation(),
        base.occupationOther(),
        base.education(),
        base.educationOther(),
        base.incomeBand());
  }

  private static RawAnswers copyRisk(RawAnswers base, String risk) {
    return new RawAnswers(
        base.policyFor(),
        base.lifePresent(),
        base.lifeName(),
        base.lifeDob(),
        base.lifeGender(),
        base.lifeMobile(),
        base.lifeEmail(),
        base.lifeStage(),
        risk,
        base.goal(),
        base.occupation(),
        base.occupationOther(),
        base.education(),
        base.educationOther(),
        base.incomeBand());
  }

  private static RawAnswers copyGoal(RawAnswers base, String goal) {
    return new RawAnswers(
        base.policyFor(),
        base.lifePresent(),
        base.lifeName(),
        base.lifeDob(),
        base.lifeGender(),
        base.lifeMobile(),
        base.lifeEmail(),
        base.lifeStage(),
        base.risk(),
        goal,
        base.occupation(),
        base.occupationOther(),
        base.education(),
        base.educationOther(),
        base.incomeBand());
  }

  private static RawAnswers copyWork(
      RawAnswers base,
      String occupation,
      String occupationOther,
      String education,
      String educationOther,
      String income) {
    return new RawAnswers(
        base.policyFor(),
        base.lifePresent(),
        base.lifeName(),
        base.lifeDob(),
        base.lifeGender(),
        base.lifeMobile(),
        base.lifeEmail(),
        base.lifeStage(),
        base.risk(),
        base.goal(),
        occupation,
        occupationOther,
        education,
        educationOther,
        income);
  }

  private static final class FixedMasters implements Masters {
    private final Set<String> occupations =
        Set.of("Salaried", "Self-employed", "Professional", "Retired", "Others");
    private final Set<String> educations =
        Set.of("Below graduate", "Graduate", "Postgraduate", "Professional", "Others");

    @Override
    public boolean occupation(String value) {
      return occupations.contains(value);
    }

    @Override
    public boolean education(String value) {
      return educations.contains(value);
    }

    @Override
    public boolean incomeBand(String label) {
      return BrdCopy.BAND_5_10.equals(label);
    }

    @Override
    public Optional<IncomeBand> income(String label) {
      return incomeBand(label) ? Optional.of(new IncomeBand(label, 500000)) : Optional.empty();
    }
  }
}
