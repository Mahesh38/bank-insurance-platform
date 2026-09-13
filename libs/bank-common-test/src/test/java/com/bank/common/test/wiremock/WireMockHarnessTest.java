package com.bank.common.test.wiremock;

import com.bank.common.test.PyramidTags;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(PyramidTags.UNIT)
class WireMockHarnessTest {

    @Test
    void start_exposesTwoIndependentServers() throws Exception {
        try (WireMockHarness harness = WireMockHarness.start()) {
            harness.onesb().stubFor(get(urlEqualTo("/onesb/ping"))
                    .willReturn(aResponse().withStatus(200).withBody("onesb-ok")));
            harness.persistence().stubFor(get(urlEqualTo("/persistence/ping"))
                    .willReturn(aResponse().withStatus(200).withBody("persistence-ok")));

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> onesb = client.send(
                    HttpRequest.newBuilder(URI.create(harness.onesb().baseUrl() + "/onesb/ping")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> persistence = client.send(
                    HttpRequest.newBuilder(URI.create(harness.persistence().baseUrl() + "/persistence/ping")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(onesb.statusCode()).isEqualTo(200);
            assertThat(onesb.body()).isEqualTo("onesb-ok");
            assertThat(persistence.statusCode()).isEqualTo(200);
            assertThat(persistence.body()).isEqualTo("persistence-ok");

            harness.reset();
            assertThat(harness.onesb().findAll(WireMock.getRequestedFor(urlEqualTo("/onesb/ping")))).isEmpty();
        }
    }
}
