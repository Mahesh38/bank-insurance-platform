package com.bank.insurance.onesb.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.domain.Lob;
import com.bank.common.error.ErrorCodes;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FUNC-028")
@Tag("unit")
class LifeQuoteValidatorTest {

  @Test
  void validTermSavingUlip_pass() {
    for (Lob lob : List.of(Lob.TERM, Lob.SAVING, Lob.ULIP)) {
      assertThat(LifeQuoteValidator.validate(base(lob))).isEmpty();
    }
  }

  @Test
  void rejectsUnknownGenderAndFutureDobAndBlankPincode() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.TERM,
            "MULTI",
            "SUM_ASSURED",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "2099-01-01",
                    "Unknown",
                    false,
                    new BigDecimal("1000000"),
                    "  ",
                    null)),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> e.field().contains("dob"))
        .anyMatch(e -> e.field().contains("gender"))
        .anyMatch(e -> e.field().contains("pincode"));
  }

  @Test
  void rejectsProposerOnlyAndInvalidFrequencyAndPptAboveTerm() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.SAVING,
            "SINGLE",
            "PREMIUM",
            new BigDecimal("5000000"),
            new BigDecimal("12000"),
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "PROPOSER",
                    1,
                    "1990-01-15",
                    "F",
                    false,
                    new BigDecimal("1000000"),
                    "400001",
                    "Spouse")),
            Map.of("savingsProductType", "ULIP"),
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor",
            new CreateQuoteCommand.ProductSelection(
                "BALIC", List.of("345"), null, null, null, 10, 20, "Weekly", "2"));

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> "members".equals(e.field()))
        .anyMatch(e -> e.field().contains("premiumPaymentTerm"))
        .anyMatch(e -> e.field().contains("premiumFrequency"));
  }

  @Test
  void rejectsInvalidSavingsFilterAndMissingAgent() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.SAVING,
            "MULTI",
            "SUM_ASSURED",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            Map.of("savingsProductType", "Endowment"),
            new CreateQuoteCommand.DistributionContext(null, "  ", "B2C"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> e.field().contains("savingsProductType"))
        .anyMatch(e -> e.field().contains("agentId"));
  }

  @Test
  void rejectsOmittedSavingsProductTypeOnSaving() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.SAVING,
            "MULTI",
            "PREMIUM",
            new BigDecimal("5000000"),
            new BigDecimal("12000"),
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2C"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> e.field().contains("savingsProductType"));
  }

  @Test
  void healthIsUnsupported() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.HEALTH,
            null,
            null,
            new BigDecimal("1"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> ErrorCodes.UNSUPPORTED_LOB.equals(e.code()));
  }

  @Test
  void rejectsNullCommandNullLobUnknownModeAndCategory() {
    assertThat(LifeQuoteValidator.validate(null))
        .anyMatch(e -> ErrorCodes.MISSING_REQUIRED_FIELD.equals(e.code()));

    CreateQuoteCommand noLob =
        new CreateQuoteCommand(
            null,
            "UNKNOWN",
            "LUMPSUM",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(noLob))
        .anyMatch(e -> "lob".equals(e.field()))
        .anyMatch(e -> "mode".equals(e.field()))
        .anyMatch(e -> "category".equals(e.field()));
  }

  @Test
  void rejectsMissingAndNonPositiveMoneyAndEmptyMembers() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.TERM,
            "MULTI",
            "PREMIUM",
            new BigDecimal("-1"),
            new BigDecimal("0"),
            List.of(),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> "sumAssured".equals(e.field()))
        .anyMatch(e -> "premiumAmount".equals(e.field()))
        .anyMatch(e -> "members".equals(e.field()));

    CreateQuoteCommand missingSum =
        new CreateQuoteCommand(
            Lob.TERM,
            "MULTI",
            "PREMIUM",
            null,
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            null,
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(missingSum))
        .anyMatch(e -> "sumAssured".equals(e.field()))
        .anyMatch(e -> "premiumAmount".equals(e.field()));
  }

  @Test
  void rejectsMemberRoleIncomePincodeAgeAndUnparseableDob() {
    CreateQuoteCommand command =
        new CreateQuoteCommand(
            Lob.TERM,
            "SINGLE",
            "SUM_ASSURED",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "DEPENDENT",
                    1,
                    "2010-01-15",
                    "M",
                    false,
                    new BigDecimal("0"),
                    "000001",
                    "Child"),
                new CreateQuoteCommand.MemberDetail(
                    "PROPOSER", 2, "not-a-date", "F", false, null, "40000", null)),
            null,
            new CreateQuoteCommand.DistributionContext("E1", null, "WEB"),
            "j-1",
            null,
            "idem",
            "actor",
            new CreateQuoteCommand.ProductSelection(
                "BALIC", List.of("345"), null, null, null, 90, 2, "M", "1"));

    assertThat(LifeQuoteValidator.validate(command))
        .anyMatch(e -> e.field().contains("role"))
        .anyMatch(e -> e.field().contains("annualIncome"))
        .anyMatch(e -> e.field().contains("pincode"))
        .anyMatch(e -> e.field().contains("dob"))
        .anyMatch(e -> e.field().contains("relationship"))
        .anyMatch(e -> e.field().contains("policyTerm"))
        .anyMatch(e -> e.field().contains("channelType"));
  }

  @Test
  void rejectsEmptySavingsListAndAcceptsAliasModes() {
    CreateQuoteCommand emptyList =
        new CreateQuoteCommand(
            Lob.SAVING,
            "MQ",
            "SUM_ASSURED",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            Map.of("savingsProductType", List.of()),
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(emptyList))
        .anyMatch(e -> e.field().contains("savingsProductType"));

    CreateQuoteCommand listInvalid =
        new CreateQuoteCommand(
            Lob.SAVING,
            "MULTI",
            "SUM_ASSURED",
            new BigDecimal("5000000"),
            null,
            List.of(
                new CreateQuoteCommand.MemberDetail(
                    "LIFE_ASSURED",
                    1,
                    "1990-01-15",
                    "M",
                    false,
                    new BigDecimal("1000000"),
                    "400001")),
            Map.of("savingsProductType", java.util.Arrays.asList("Endowment", null)),
            new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
            "j-1",
            null,
            "idem",
            "actor");

    assertThat(LifeQuoteValidator.validate(listInvalid))
        .anyMatch(e -> e.field().contains("savingsProductType"));

    assertThat(LifeQuoteValidator.isSingleQuote("SINGLE_QUOTE")).isTrue();
    assertThat(LifeQuoteValidator.isSingleQuote("sq")).isTrue();
    assertThat(LifeQuoteValidator.isSingleQuote("  ")).isFalse();
  }

  private static CreateQuoteCommand base(Lob lob) {
    return new CreateQuoteCommand(
        lob,
        "MULTI",
        "SUM_ASSURED",
        new BigDecimal("5000000"),
        null,
        List.of(
            new CreateQuoteCommand.MemberDetail(
                "LIFE_ASSURED", 1, "1990-01-15", "M", false, new BigDecimal("1000000"), "400001")),
        lob == Lob.SAVING ? Map.of("savingsProductType", "ULIP") : null,
        new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
        "j-1",
        null,
        "idem",
        "actor");
  }
}
