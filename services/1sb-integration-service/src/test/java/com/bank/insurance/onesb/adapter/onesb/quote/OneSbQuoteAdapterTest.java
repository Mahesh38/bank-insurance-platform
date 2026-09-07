package com.bank.insurance.onesb.adapter.onesb.quote;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.common.domain.Lob;
import com.bank.common.domain.QuoteOffer;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import com.bank.insurance.onesb.lob.life.term.TermQuoteHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("FUNC-002")
class OneSbQuoteAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private OneSbQuoteAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        SecretProvider secrets = mock(SecretProvider.class);
        when(secrets.getDistributorId()).thenReturn("TEST_DIST");
        TermQuoteHandler handler = new TermQuoteHandler(secrets);
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(handler), TestErrors.ONESB);

        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(http1Factory())
                .build();
        OneSbHttpClient httpClient = new OneSbHttpClient(restClient);
        adapter = new OneSbQuoteAdapter(httpClient, registry, new ObjectMapper());
    }

    private static JdkClientHttpRequestFactory http1Factory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    @Test
    void submitQuote_postsGivenPathAndPayload_extractsReqId() {
        wireMock.stubFor(post(urlEqualTo("/insurance/lifeterm/v1/quote"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-42\",\"data\":{}}")));

        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.TERM, null, null, new BigDecimal("5000000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LIFE_ASSURED", 1, "1990-01-15", "M", false, null, null)),
                null,
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem", "actor"
        );
        SecretProvider secrets = mock(SecretProvider.class);
        when(secrets.getDistributorId()).thenReturn("TEST_DIST");
        Object payload = new TermQuoteHandler(secrets).buildSubmitPayload(command);

        String reqId = adapter.submitQuote("job-1", "/insurance/lifeterm/v1/quote", payload);

        assertThat(reqId).isEqualTo("REQ-42");
        wireMock.verify(postRequestedFor(urlEqualTo("/insurance/lifeterm/v1/quote")));
    }

    @Test
    void pollQuoteResult_completeWithOffers() {
        wireMock.stubFor(get(urlEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "isPollComplete": true,
                                    "quote": [{
                                      "offerId": "off-1",
                                      "insurerCode": "HDFC",
                                      "insurerName": "HDFC Life",
                                      "productCode": "T1",
                                      "productName": "Term One",
                                      "premiumAmount": 12300,
                                      "premiumFrequency": "YEARLY",
                                      "sumAssured": 5000000,
                                      "outOfBound": false
                                    }]
                                  }
                                }
                                """)));

        assertThat(adapter.isPollComplete("job-1", "REQ-1", "TERM")).isTrue();
        List<QuoteOffer> offers = adapter.pollQuoteResult("job-1", "REQ-1", "TERM");
        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().insurerCode()).isEqualTo("HDFC");
        assertThat(offers.getFirst().premiumAmount()).isEqualByComparingTo("12300");
        assertThat(offers.getFirst().errorSummary()).isNull();
    }

    @Test
    void pollQuoteResult_partialMapping_whenOneOfferHasErrorSummary() {
        wireMock.stubFor(get(urlEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "isPollComplete": true,
                                    "quote": [
                                      {
                                        "offerId": "off-ok",
                                        "insurerCode": "ICICI",
                                        "productCode": "P1",
                                        "productName": "OK Product",
                                        "premiumAmount": 9000,
                                        "sumAssured": 5000000
                                      },
                                      {
                                        "offerId": "off-bad",
                                        "insurerCode": "OTHER",
                                        "productCode": "P2",
                                        "productName": "Fail Product",
                                        "errorSummary": "UW decline"
                                      }
                                    ]
                                  }
                                }
                                """)));

        List<QuoteOffer> offers = adapter.pollQuoteResult("job-1", "REQ-2", "TERM");
        assertThat(offers).hasSize(2);
        assertThat(offers.stream().filter(o -> o.errorSummary() == null)).hasSize(1);
        assertThat(offers.stream().filter(o -> "UW decline".equals(o.errorSummary()))).hasSize(1);
    }

    @Test
    void submitQuote_missingBody_throws() {
        wireMock.stubFor(post(urlEqualTo("/insurance/lifeterm/v1/quote"))
                .willReturn(aResponse().withStatus(204)));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> adapter.submitQuote("job-1", "/insurance/lifeterm/v1/quote", Map.of()));
    }

    @Test
    void submitQuote_reqIdNestedUnderData_isExtracted() {
        wireMock.stubFor(post(urlEqualTo("/insurance/lifeterm/v1/quote"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"reqId\":\"REQ-NESTED\"}}")));

        String reqId = adapter.submitQuote("job-1", "/insurance/lifeterm/v1/quote", Map.of());

        assertThat(reqId).isEqualTo("REQ-NESTED");
    }

    @Test
    void submitQuote_missingReqId_throws() {
        wireMock.stubFor(post(urlEqualTo("/insurance/lifeterm/v1/quote"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{}}")));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> adapter.submitQuote("job-1", "/insurance/lifeterm/v1/quote", Map.of()));
    }

    @Test
    void isPollComplete_flagAtRoot_textualTrue() {
        wireMock.stubFor(get(urlEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"isPollComplete\":\"true\",\"data\":{}}")));

        assertThat(adapter.isPollComplete("job-1", "REQ-3", "TERM")).isTrue();
    }

    @Test
    void isPollComplete_malformedBody_returnsFalse() {
        wireMock.stubFor(get(urlEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not-json")));

        assertThat(adapter.isPollComplete("job-1", "REQ-4", "TERM")).isFalse();
    }

    @Test
    void isPollComplete_noFlagNoOffers_returnsFalse() {
        wireMock.stubFor(get(urlEqualTo("/insurance/lifeterm/v1/quote/poll/REQ-5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{}}")));

        assertThat(adapter.isPollComplete("job-1", "REQ-5", "TERM")).isFalse();
    }

    @Test
    void parseOffers_nullOrBlankBody_returnsEmpty() {
        assertThat(adapter.parseOffers(null)).isEmpty();
        assertThat(adapter.parseOffers("  ")).isEmpty();
    }

    @Test
    void parseOffers_malformedJson_returnsEmpty() {
        assertThat(adapter.parseOffers("{not-json")).isEmpty();
    }

    @Test
    void parseOffers_productsArray_mapped() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "products": [{
                      "productId": "P9",
                      "manufacturerCode": "SBI",
                      "premium": {"amount": 4500},
                      "outOfBound": "Yes",
                      "status": "QUOTED"
                    }]
                  }
                }
                """);

        assertThat(offers).hasSize(1);
        QuoteOffer offer = offers.getFirst();
        assertThat(offer.productCode()).isEqualTo("P9");
        assertThat(offer.insurerCode()).isEqualTo("SBI");
        assertThat(offer.premiumAmount()).isEqualByComparingTo("4500");
        assertThat(offer.outOfBound()).isTrue();
        assertThat(offer.offerStatus()).isEqualTo("QUOTED");
    }

    @Test
    void parseOffers_errorWithBlankMessage_skipped() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "errors": [{"manufacturerId": "HDFC", "message": "   "}]
                  }
                }
                """);

        assertThat(offers).isEmpty();
    }

    @Test
    void parseOffers_errorForInsurerWithNoSuccessOffer_addsErrorRow() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "errors": [{"manufacturerId": "HDFC", "manufacturerName": "HDFC Life", "message": "Rejected"}]
                  }
                }
                """);

        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().errorSummary()).isEqualTo("Rejected");
        assertThat(offers.getFirst().offerStatus()).isEqualTo("ERROR");
    }

    @Test
    void parseOffers_errorForInsurerWithExistingSuccessOffer_addsSeparateErrorRow() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "quote": [{"offerId": "off-1", "insurerCode": "HDFC", "premiumAmount": 100}],
                    "errors": [{"manufacturerId": "HDFC", "message": "Partial reject"}]
                  }
                }
                """);

        assertThat(offers).hasSize(2);
        assertThat(offers.stream().filter(o -> "Partial reject".equals(o.errorSummary()))).hasSize(1);
    }

    @Test
    void parseOffers_topLevelErrorsFallback_mapped() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {},
                  "errors": [{"code": "MOTOR", "errorMessage": "top-level reject"}]
                }
                """);

        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().errorSummary()).isEqualTo("top-level reject");
    }

    @Test
    void parseOffers_errorSummaryFromNestedErrorsArrayOnOffer() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "quote": [{
                      "offerId": "off-x",
                      "insurerCode": "ICICI",
                      "errors": [{"message": "nested reject"}]
                    }]
                  }
                }
                """);

        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().errorSummary()).isEqualTo("nested reject");
    }

    @Test
    void parseOffers_unparseableTextualPremium_fallsBackToNull() {
        List<QuoteOffer> offers = adapter.parseOffers("""
                {
                  "data": {
                    "quote": [{"offerId": "off-y", "insurerCode": "ICICI", "premiumAmount": "not-a-number"}]
                  }
                }
                """);

        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().premiumAmount()).isNull();
    }
}
