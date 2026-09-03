package com.bank.insurance.onesb.lob.life.saving;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("FUNC-015")
class SavingQuoteHandlerTest {

    private SavingQuoteHandler handler;

    @BeforeEach
    void setUp() {
        SecretProvider secrets = mock(SecretProvider.class);
        when(secrets.getDistributorId()).thenReturn("TEST_DIST");
        handler = new SavingQuoteHandler(secrets);
    }

    @Test
    void supportedLob_isSaving() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.SAVING);
    }

    @Test
    void pathsAndProductToken() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.SAVING, "MULTI", "SUM_ASSURED", new BigDecimal("1000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1985-06-01", "F", false,
                        new BigDecimal("900000"), "560001")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "A1", "B2B"),
                "j-s", null, "idem", "actor"
        );

        LifeQuoteRequest payload = handler.buildSubmitPayload(command);
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifesaving/v1/quote");
        assertThat(handler.pollPath("R1")).isEqualTo("/insurance/lifesaving/v1/quote/poll/R1");
        assertThat(payload.product().product()).isEqualTo("LifeSaving");
        assertThat(payload.distributor().distributorID()).isEqualTo("TEST_DIST");
    }
}
