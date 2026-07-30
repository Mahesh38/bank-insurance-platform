package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("FUNC-002")
class TermQuoteHandlerTest {

    private TermQuoteHandler handler;

    @BeforeEach
    void setUp() {
        SecretProvider secrets = mock(SecretProvider.class);
        when(secrets.getDistributorId()).thenReturn("TEST_DIST");
        handler = new TermQuoteHandler(secrets);
    }

    @Test
    void supportedLob_isTerm() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.TERM);
    }

    @Test
    void submitAndPollPaths() {
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifeterm/v1/quote");
        assertThat(handler.pollPath("REQ-1")).isEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildSubmitPayload_mapsMinimalTermFields() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.TERM, "MULTI", "SUM_ASSURED", new BigDecimal("5000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1990-01-15", "M", true,
                        new BigDecimal("1200000"), "400001")),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor"
        );

        Map<String, Object> payload = (Map<String, Object>) handler.buildSubmitPayload(command);

        assertThat(payload.get("typeOfQuote")).isEqualTo("Multi-Quote");
        assertThat(payload.get("quoteCategory")).isEqualTo("Sum Assured");

        Map<String, Object> distributor = (Map<String, Object>) payload.get("distributor");
        assertThat(distributor.get("distributorID")).isEqualTo("TEST_DIST");
        assertThat(distributor.get("agentID")).isEqualTo("109337");
        assertThat(distributor.get("channelType")).isEqualTo("B2B");

        Map<String, Object> personal = (Map<String, Object>) payload.get("personalInformation");
        List<Map<String, Object>> individuals =
                (List<Map<String, Object>>) personal.get("individualDetails");
        assertThat(individuals).hasSize(1);
        Map<String, Object> member = individuals.getFirst();
        assertThat(member.get("dateOfBirth")).isEqualTo("1990-01-15");
        assertThat(member.get("gender")).isEqualTo("Male");
        assertThat(member.get("tobacco")).isEqualTo("Yes");
        assertThat(member.get("quoteAmount")).isEqualTo(new BigDecimal("5000000"));
        assertThat(member.get("zipCode")).isEqualTo("400001");

        Map<String, Object> product = (Map<String, Object>) payload.get("product");
        assertThat(product.get("product")).isEqualTo("LifeTerm");
    }
}
