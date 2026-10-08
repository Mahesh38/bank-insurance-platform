package com.bank.identity.provider.adapter.adverify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.identity.provider.config.BankAdVerifyProperties;
import com.bank.identity.provider.config.BankAdVerifyProperties.Http;
import com.bank.identity.provider.config.BankAdVerifyProperties.Stub;
import com.bank.identity.provider.config.BankAdVerifyProperties.StubUser;
import com.bank.identity.provider.domain.WorkforceCredentialVerifier.AdVerifyCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("IAM-001")
@Tag("unit")
class BankAdVerifyAdapterTest {

  @Test
  void stubAcceptsActiveDirectoryAccount() {
    var result = stubAdapter().verify(new AdVerifyCommand("EMP001", "local-stub-password"));

    assertThat(result.accepted()).isTrue();
    assertThat(result.employeeId()).isEqualTo("EMP001");
    assertThat(result.email()).isEqualTo("emp001@example.test");
  }

  @Test
  void stubRejectsUnknownWrongPasswordAndInactiveTheSameWay() {
    BankAdVerifyAdapter adapter = stubAdapter();

    assertThat(adapter.verify(new AdVerifyCommand("EMP001", "wrong")).accepted()).isFalse();
    assertThat(adapter.verify(new AdVerifyCommand("UNKNOWN", "local-stub-password")).accepted())
        .isFalse();
    assertThat(adapter.verify(new AdVerifyCommand("EMP002", "local-stub-password")).accepted())
        .isFalse();
  }

  @Test
  void httpParsesBareTrueAndDoesNotEchoPasswordInCommandToString() {
    TestHttp http = httpAdapter();
    http.server()
        .expect(requestTo("http://ad.example.test/ad/authenticate"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.employeeId").value("EMP001"))
        .andExpect(jsonPath("$.password").value("secret"))
        .andExpect(header("X-API-Key", "test-key"))
        .andRespond(withSuccess("true", MediaType.TEXT_PLAIN));

    var result = http.adapter().verify(new AdVerifyCommand("EMP001", "secret"));

    assertThat(result.accepted()).isTrue();
    assertThat(new AdVerifyCommand("EMP001", "secret").toString()).doesNotContain("secret");
    http.server().verify();
  }

  @Test
  void httpParsesJsonFalseAnd401AsRejected() {
    TestHttp http = httpAdapter();
    http.server()
        .expect(requestTo("http://ad.example.test/ad/authenticate"))
        .andRespond(withSuccess("{\"authenticated\":false}", MediaType.APPLICATION_JSON));

    assertThat(http.adapter().verify(new AdVerifyCommand("EMP001", "secret")).accepted()).isFalse();

    TestHttp unauthorized = httpAdapter();
    unauthorized
        .server()
        .expect(requestTo("http://ad.example.test/ad/authenticate"))
        .andRespond(withUnauthorizedRequest());
    assertThat(unauthorized.adapter().verify(new AdVerifyCommand("EMP001", "secret")).accepted())
        .isFalse();
  }

  @Test
  void http5xxFailsClosed() {
    TestHttp http = httpAdapter();
    http.server()
        .expect(requestTo("http://ad.example.test/ad/authenticate"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> http.adapter().verify(new AdVerifyCommand("EMP001", "secret")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE);
  }

  @Test
  void stubModeIsForbiddenInProd() {
    BankAdVerifyAdapter adapter = stubAdapter();
    Environment environment = mock(Environment.class);
    when(environment.matchesProfiles("prod")).thenReturn(true);
    var prodAdapter =
        new BankAdVerifyAdapter(
            stubProperties(), RestClient.create(), new ObjectMapper(), errors(), environment);

    assertThatThrownBy(prodAdapter::rejectStubInProduction)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("forbidden");
    adapter.rejectStubInProduction();
  }

  private static BankAdVerifyAdapter stubAdapter() {
    Environment environment = mock(Environment.class);
    when(environment.matchesProfiles("prod")).thenReturn(false);
    return new BankAdVerifyAdapter(
        stubProperties(), RestClient.create(), new ObjectMapper(), errors(), environment);
  }

  private static TestHttp httpAdapter() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    Environment environment = mock(Environment.class);
    when(environment.matchesProfiles("prod")).thenReturn(false);
    var properties =
        new BankAdVerifyProperties(
            "http",
            Duration.ofSeconds(3),
            Duration.ofSeconds(5),
            new Http(
                URI.create("http://ad.example.test"), "/ad/authenticate", "test-key", "X-API-Key"),
            new Stub(List.of()));
    return new TestHttp(
        server,
        new BankAdVerifyAdapter(
            properties, builder.build(), new ObjectMapper(), errors(), environment));
  }

  private static BankAdVerifyProperties stubProperties() {
    return new BankAdVerifyProperties(
        "stub",
        Duration.ofSeconds(3),
        Duration.ofSeconds(5),
        new Http(URI.create("http://127.0.0.1:9"), "/ad/authenticate", "", "X-API-Key"),
        new Stub(
            List.of(
                new StubUser(
                    "EMP001", "local-stub-password", true, "emp001@example.test", "EMP001"),
                new StubUser(
                    "EMP002", "local-stub-password", false, "emp002@example.test", "EMP002"))));
  }

  private static ServiceErrors errors() {
    return ServiceErrors.of("idp-adapter", PlatformLayer.L5);
  }

  private record TestHttp(MockRestServiceServer server, BankAdVerifyAdapter adapter) {}
}
