package com.bank.insurance.onesb.application.validation;

import com.bank.common.domain.Lob;
import com.bank.common.error.ErrorCodes;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.TERM, "MULTI", "SUM_ASSURED", new BigDecimal("5000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "2099-01-01", "Unknown", false,
                        new BigDecimal("1000000"), "  ", null)),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor");

        assertThat(LifeQuoteValidator.validate(command))
                .anyMatch(e -> e.field().contains("dob"))
                .anyMatch(e -> e.field().contains("gender"))
                .anyMatch(e -> e.field().contains("pincode"));
    }

    @Test
    void rejectsProposerOnlyAndInvalidFrequencyAndPptAboveTerm() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.SAVING, "SINGLE", "PREMIUM", new BigDecimal("5000000"), new BigDecimal("12000"),
                List.of(new CreateQuoteCommand.MemberDetail(
                        "PROPOSER", 1, "1990-01-15", "F", false,
                        new BigDecimal("1000000"), "400001", "Spouse")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor",
                new CreateQuoteCommand.ProductSelection(
                        "BALIC", List.of("345"), null, null, null, 10, 20, "Weekly", "2"));

        assertThat(LifeQuoteValidator.validate(command))
                .anyMatch(e -> "members".equals(e.field()))
                .anyMatch(e -> e.field().contains("premiumPaymentTerm"))
                .anyMatch(e -> e.field().contains("premiumFrequency"));
    }

    @Test
    void rejectsInvalidSavingsFilterAndMissingAgent() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.SAVING, "MULTI", "SUM_ASSURED", new BigDecimal("5000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1990-01-15", "M", false,
                        new BigDecimal("1000000"), "400001")),
                Map.of("savingsProductType", "Endowment"),
                new CreateQuoteCommand.DistributionContext(null, "  ", "B2C"),
                "j-1", null, "idem", "actor");

        assertThat(LifeQuoteValidator.validate(command))
                .anyMatch(e -> e.field().contains("savingsProductType"))
                .anyMatch(e -> e.field().contains("agentId"));
    }

    @Test
    void healthIsUnsupported() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.HEALTH, null, null, new BigDecimal("1"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1990-01-15", "M", false,
                        new BigDecimal("1000000"), "400001")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor");

        assertThat(LifeQuoteValidator.validate(command))
                .anyMatch(e -> ErrorCodes.UNSUPPORTED_LOB.equals(e.code()));
    }

    private static CreateQuoteCommand base(Lob lob) {
        return new CreateQuoteCommand(
                lob, "MULTI", "SUM_ASSURED", new BigDecimal("5000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1990-01-15", "M", false,
                        new BigDecimal("1000000"), "400001")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor");
    }
}
