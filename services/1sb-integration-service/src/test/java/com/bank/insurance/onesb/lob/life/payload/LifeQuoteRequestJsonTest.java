package com.bank.insurance.onesb.lob.life.payload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-015")
class LifeQuoteRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void distributor_serialisesBothAgentIdSpellingsAndSalesChannel() throws Exception {
        LifeQuoteRequest request = new LifeQuoteRequest(
                "Multi-Quote",
                "Premium",
                "withoutBI",
                "Yes",
                new LifeQuoteRequest.AdditionalSetup("INR", "IN"),
                new LifeQuoteRequest.Distributor("BCIBL", "109337", "B2B", "Online"),
                new LifeQuoteRequest.PersonalInformation(List.of(
                        new LifeQuoteRequest.IndividualDetail(
                                "Life Assured", 1, "Male", "1990-04-12", "No",
                                new BigDecimal("1500000"), "400001", new BigDecimal("100000")))),
                LifeQuoteRequest.Product.saving("LifeSave", List.of("ULIP"))
        );

        JsonNode json = mapper.readTree(mapper.writeValueAsString(request));
        JsonNode distributor = json.path("distributor");
        assertThat(distributor.path("distributorID").asText()).isEqualTo("BCIBL");
        assertThat(distributor.path("agentId").asText()).isEqualTo("109337");
        assertThat(distributor.path("agentID").asText()).isEqualTo("109337");
        assertThat(distributor.path("channelType").asText()).isEqualTo("B2B");
        assertThat(distributor.path("salesChannel").asText()).isEqualTo("Online");
        assertThat(json.path("product").path("savingsProductType").get(0).asText()).isEqualTo("ULIP");
    }
}
