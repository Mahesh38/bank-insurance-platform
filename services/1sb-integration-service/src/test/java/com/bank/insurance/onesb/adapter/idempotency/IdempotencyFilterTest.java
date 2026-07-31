package com.bank.insurance.onesb.adapter.idempotency;

import com.bank.common.error.ErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IdempotencyFilterTest.IdempotencyProbeConfig.class)
@TestPropertySource(properties = {
        "onesb.api-key=test-api-key",
        "onesb.api-secret=test-api-secret",
        "onesb.distributor-id=TEST_DIST",
        "insurance.secrets.source=PROPERTIES",
        "bank.persistence.base-url=http://localhost:8081"
})
class IdempotencyFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryIdempotencyStore store;

    @Autowired
    private AtomicInteger probeInvocationCount;

    @BeforeEach
    void clearStore() {
        store.clear();
        probeInvocationCount.set(0);
    }

    @Test
    void missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/v1/_idempotency_probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.MISSING_IDEMPOTENCY_KEY)));
        assertThat(probeInvocationCount.get()).isZero();
    }

    @Test
    void firstPostStores_secondIdenticalReplaysWithoutReinvoking() throws Exception {
        String body = "{\"n\":42}";

        mockMvc.perform(post("/v1/_idempotency_probe")
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true,\"n\":42}"));

        assertThat(probeInvocationCount.get()).isEqualTo(1);

        mockMvc.perform(post("/v1/_idempotency_probe")
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true,\"n\":42}"));

        assertThat(probeInvocationCount.get()).isEqualTo(1);
    }

    @Test
    void sameKeyDifferentBody_returns409() throws Exception {
        mockMvc.perform(post("/v1/_idempotency_probe")
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/_idempotency_probe")
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(ErrorCodes.IDEMPOTENCY_CONFLICT)));

        assertThat(probeInvocationCount.get()).isEqualTo(1);
    }

    @Test
    void getDoesNotRequireIdempotencyKey() throws Exception {
        mockMvc.perform(get("/v1/_idempotency_probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    @org.junit.jupiter.api.Tag("FUNC-001")
    void masterDataPost_withoutIdempotencyKey_isNotBlocked() throws Exception {
        mockMvc.perform(post("/v1/master-data/_idempotency_probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"n\":7}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ok\":true,\"n\":7}"));
        assertThat(probeInvocationCount.get()).isEqualTo(1);
    }

    @TestConfiguration
    static class IdempotencyProbeConfig {

        @Bean
        AtomicInteger probeInvocationCount() {
            return new AtomicInteger();
        }

        @Bean
        IdempotencyProbeController idempotencyProbeController(AtomicInteger probeInvocationCount) {
            return new IdempotencyProbeController(probeInvocationCount);
        }
    }

    @RestController
    static class IdempotencyProbeController {

        private final AtomicInteger invocations;

        IdempotencyProbeController(AtomicInteger invocations) {
            this.invocations = invocations;
        }

        @PostMapping("/v1/_idempotency_probe")
        ProbeResponse post(@RequestBody ProbeRequest request) {
            invocations.incrementAndGet();
            return new ProbeResponse(true, request.n());
        }

        @PostMapping("/v1/master-data/_idempotency_probe")
        ProbeResponse masterDataPost(@RequestBody ProbeRequest request) {
            invocations.incrementAndGet();
            return new ProbeResponse(true, request.n());
        }

        @GetMapping("/v1/_idempotency_probe")
        String get() {
            return "pong";
        }
    }

    record ProbeRequest(int n) {}

    record ProbeResponse(boolean ok, int n) {}
}
