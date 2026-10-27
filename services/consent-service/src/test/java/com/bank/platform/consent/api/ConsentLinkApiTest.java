package com.bank.platform.consent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.security.internal.InternalCallerAuthFilter;
import com.bank.platform.consent.ConsentApplication;
import com.bank.platform.consent.config.PublicLinkRateLimitFilter;
import com.bank.platform.consent.config.PublicLinkRateLimiter;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.LinkPorts;
import com.bank.platform.consent.domain.QuoteShareStatement;
import com.bank.platform.consent.domain.Ulids;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = {ConsentApplication.class, ConsentLinkApiTest.CaptureConfig.class})
@AutoConfigureMockMvc
class ConsentLinkApiTest {

  private static final AtomicReference<Instant> NOW =
      new AtomicReference<>(Instant.parse("2026-11-01T00:00:00Z"));
  private static final AtomicReference<String> MOBILE = new AtomicReference<>();
  private static final AtomicReference<String> EMAIL = new AtomicReference<>();
  private static final AtomicReference<String> URL = new AtomicReference<>();

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ConsentPorts.ConsentStore consents;
  @Autowired PublicLinkRateLimiter limiter;
  @Autowired WebApplicationContext context;

  @Autowired
  @Qualifier("internalCallerAuthFilter")
  FilterRegistrationBean<InternalCallerAuthFilter> internalFilter;

  @Autowired
  @Qualifier("publicLinkRateLimitFilter")
  FilterRegistrationBean<PublicLinkRateLimitFilter> publicFilter;

  @BeforeEach
  void reset() {
    NOW.set(Instant.parse("2026-11-01T00:00:00Z"));
    MOBILE.set(null);
    EMAIL.set(null);
    URL.set(null);
    limiter.reset();
  }

  @Test
  void issueDoesNotReturnTheTokenAndTheCustomerCanDisagree() throws Exception {
    String customer = ulid();
    String assessment = ulid();
    MvcResult issued = issue(customer, assessment);
    String body = issued.getResponse().getContentAsString();
    String token = token();
    assertThat(body).doesNotContain(token);
    assertThat(MOBILE.get()).isEqualTo("9876543210");
    assertThat(EMAIL.get()).isEqualTo("customer@example.com");
    assertThat(URL.get()).contains("/public/v1/consent-links/" + token);
    mvc.perform(get("/public/v1/consent-links/" + token))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(QuoteShareStatement.TEXT)))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Agree")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Disagree")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("9876543210"))))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    org.hamcrest.Matchers.containsString("default-src 'none'")));

    mvc.perform(
            post("/public/v1/consent-links/" + token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("decision", "DISAGREE"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "You did not agree to share your personal details.")));
    mvc.perform(get("/internal/v1/consent-links").param("assessmentId", assessment))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("DISAGREED"));
    assertThat(consents.head(customer, QuoteShareStatement.PURPOSE, null)).isEmpty();
  }

  @Test
  void agreeWritesQuoteShareWithoutAnOtpAndASecondDecisionStaysRecorded() throws Exception {
    String customer = ulid();
    String assessment = ulid();
    issue(customer, assessment);
    String token = token();
    mvc.perform(
            post("/public/v1/consent-links/" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("decision", "AGREE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kind").value("AGREED"));
    var row = consents.head(customer, QuoteShareStatement.PURPOSE, null).orElseThrow();
    assertThat(row.state()).isEqualTo("GRANTED");
    assertThat(row.acknowledgementMethod()).isEqualTo(QuoteShareStatement.ACK);
    assertThat(row.otpTxnId()).isNull();
    assertThat(row.otpVerifiedAt()).isNull();
    assertThat(row.statementText()).isEqualTo(QuoteShareStatement.TEXT);
    mvc.perform(get("/internal/v1/consent-links/quote-share").param("customerId", customer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granted").value(true));
    mvc.perform(
            post("/public/v1/consent-links/" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("decision", "AGREE"))))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("Your consent has been recorded.")));
    assertThat(
            consents
                .head(customer, QuoteShareStatement.PURPOSE, null)
                .orElseThrow()
                .recordVersion())
        .isEqualTo(1);
  }

  @Test
  void resendInvalidatesThePreviousToken() throws Exception {
    String assessment = ulid();
    issue(ulid(), assessment);
    String first = token();
    mvc.perform(
            post("/internal/v1/consent-links/resend")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("assessmentId", assessment))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sent").value(true));
    String second = token();
    assertThat(second).isNotEqualTo(first);
    mvc.perform(get("/public/v1/consent-links/" + first))
        .andExpect(status().isConflict())
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("This link has already been used.")));
    mvc.perform(get("/public/v1/consent-links/" + second))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Agree")));
  }

  @Test
  void malformedTokenIsNotFoundAndAnExpiredLinkIsGone() throws Exception {
    mvc.perform(get("/public/v1/consent-links/not-a-token"))
        .andExpect(status().isNotFound())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("This link is not valid.")));
    String assessment = ulid();
    issue(ulid(), assessment);
    String token = token();
    NOW.set(NOW.get().plus(25, ChronoUnit.HOURS));
    mvc.perform(get("/public/v1/consent-links/" + token))
        .andExpect(status().isGone())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("This link has expired.")));
  }

  @Test
  void sixthIssueIsRateLimited() throws Exception {
    String customer = ulid();
    String assessment = ulid();
    issue(customer, assessment);
    for (int i = 0; i < 4; i++) {
      mvc.perform(
              post("/internal/v1/consent-links/resend")
                  .header("X-Actor-Id", "sp-1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(Map.of("assessmentId", assessment))))
          .andExpect(status().isOk());
    }
    mvc.perform(
            post("/internal/v1/consent-links/resend")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("assessmentId", assessment))))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("OTP_ISSUE_RATE_EXCEEDED"));
  }

  @Test
  void publicPageDoesNotRequireTheInternalKeyAndTheSellerCallDoes() throws Exception {
    MockMvc plain =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(publicFilter.getFilter(), internalFilter.getFilter())
            .build();
    plain.perform(get("/public/v1/consent-links/not-a-token")).andExpect(status().isNotFound());
    plain
        .perform(
            post("/internal/v1/consent-links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicBurstIsLimited() throws Exception {
    limiter.reset();
    for (int i = 0; i < 30; i++) {
      mvc.perform(get("/public/v1/consent-links/not-a-token")).andExpect(status().isNotFound());
    }
    mvc.perform(get("/public/v1/consent-links/not-a-token"))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().string("Please wait a moment and try again."));
  }

  private MvcResult issue(String customer, String assessment) throws Exception {
    return mvc.perform(
            post("/internal/v1/consent-links")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "customerId",
                            customer,
                            "leadId",
                            ulid(),
                            "journeyId",
                            ulid(),
                            "assessmentId",
                            assessment,
                            "registeredMobile",
                            "9876543210",
                            "registeredEmail",
                            "customer@example.com"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sent").value(true))
        .andExpect(jsonPath("$.state").value("PENDING"))
        .andExpect(jsonPath("$.maskedMobile").value("+91 98*****210"))
        .andReturn();
  }

  private static String token() {
    String url = URL.get();
    return url.substring(url.lastIndexOf('/') + 1);
  }

  private static String ulid() {
    return Ulids.mint(Clock.fixed(NOW.get(), ZoneOffset.UTC));
  }

  @TestConfiguration
  static class CaptureConfig {
    @Bean
    @Primary
    Clock clock() {
      return new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return NOW.get();
        }
      };
    }

    @Bean
    @Primary
    LinkPorts.Dispatch dispatch() {
      return (mobile, email, url) -> {
        MOBILE.set(mobile);
        EMAIL.set(email);
        URL.set(url);
      };
    }
  }
}
