package com.bank.insurance.onesb.lob.life.ulip;

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

@Tag("FUNC-019")
class UlipQuoteHandlerTest {

    private UlipQuoteHandler handler;

    @BeforeEach
    void setUp() {
        SecretProvider secrets = mock(SecretProvider.class);
        when(secrets.getDistributorId()).thenReturn("TEST_DIST");
        handler = new UlipQuoteHandler(secrets);
    }

    @Test
    void supportedLob_isUlip() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.ULIP);
    }

    @Test
    void pathsAndProductToken() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", new BigDecimal("2000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1988-03-20", "M", false,
                        new BigDecimal("1500000"), "110001")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "U9", "B2B"),
                "j-u", null, "idem", "actor"
        );

        LifeQuoteRequest payload = handler.buildSubmitPayload(command);
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifeulip/v1/quote");
        assertThat(handler.pollPath("R1")).isEqualTo("/insurance/lifeulip/v1/quote/poll/R1");
        assertThat(payload.product().product()).isEqualTo("LifeUlip");
    }
}
