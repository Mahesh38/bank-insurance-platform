package com.bank.persistence.api.internal.v1;

import com.bank.persistence.entity.RawPayloadEntity;
import com.bank.persistence.repo.RawPayloadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("COMP-003")
@Tag("integration")
class RawPayloadApiTest {

    private static final String SECRET_PLAINTEXT = "COMP003_SECRET_PAYLOAD_DO_NOT_STORE_PLAIN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RawPayloadRepository rawPayloadRepository;

    @Test
    void post_storesEncryptedPayload_entityDoesNotContainPlaintext() throws Exception {
        String jobId = UUID.randomUUID().toString();
        String payloadB64 = Base64.getEncoder().encodeToString(SECRET_PLAINTEXT.getBytes(StandardCharsets.UTF_8));

        MvcResult created = mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "REQ",
                                  "operation": "POST /insurance/lifeterm/v1/quote",
                                  "lob": "TERM",
                                  "payloadBase64": "%s",
                                  "httpStatus": null
                                }
                                """.formatted(jobId, payloadB64)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payloadId", notNullValue()))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.direction", is("REQ")))
                .andExpect(jsonPath("$.operation", is("POST /insurance/lifeterm/v1/quote")))
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.encryptionKeyId", is("test-v1")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.retainUntil", is(LocalDate.now().plusYears(7).toString())))
                .andExpect(jsonPath("$.payloadBase64").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String payloadId = body.get("payloadId").asText();

        RawPayloadEntity entity = rawPayloadRepository.findById(payloadId).orElseThrow();
        String encAsUtf8 = new String(entity.getPayloadEnc(), StandardCharsets.UTF_8);
        assertThat(encAsUtf8).doesNotContain(SECRET_PLAINTEXT);
        assertThat(entity.getEncryptionKeyId()).isEqualTo("test-v1");
        assertThat(entity.getRetainUntil()).isEqualTo(LocalDate.now().plusYears(7));
    }

    @Test
    void get_decryptTrue_roundTripsPlaintextBase64() throws Exception {
        String jobId = UUID.randomUUID().toString();
        byte[] plaintext = SECRET_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
        String payloadB64 = Base64.getEncoder().encodeToString(plaintext);

        MvcResult created = mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "RES",
                                  "operation": "POST /insurance/lifeterm/v1/quote",
                                  "lob": "TERM",
                                  "payloadBase64": "%s",
                                  "httpStatus": 200
                                }
                                """.formatted(jobId, payloadB64)))
                .andExpect(status().isCreated())
                .andReturn();

        String payloadId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("payloadId").asText();

        mockMvc.perform(get("/internal/v1/raw-payloads/{payloadId}", payloadId)
                        .param("decrypt", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payloadId", is(payloadId)))
                .andExpect(jsonPath("$.encryptionKeyId", is("test-v1")))
                .andExpect(jsonPath("$.httpStatus", is(200)))
                .andExpect(jsonPath("$.payloadBase64", is(payloadB64)))
                .andExpect(jsonPath("$.retainUntil", is(LocalDate.now().plusYears(7).toString())));
    }

    @Test
    void get_withoutDecrypt_omitsPayloadBytes() throws Exception {
        String jobId = UUID.randomUUID().toString();
        String payloadB64 = Base64.getEncoder().encodeToString("meta-only".getBytes(StandardCharsets.UTF_8));

        MvcResult created = mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "REQ",
                                  "operation": "GET /status",
                                  "lob": "TERM",
                                  "payloadBase64": "%s"
                                }
                                """.formatted(jobId, payloadB64)))
                .andExpect(status().isCreated())
                .andReturn();

        String payloadId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("payloadId").asText();

        mockMvc.perform(get("/internal/v1/raw-payloads/{payloadId}", payloadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payloadId", is(payloadId)))
                .andExpect(jsonPath("$.payloadBase64").doesNotExist());
    }

    @Test
    void post_badDirection_returns400() throws Exception {
        mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "BAD",
                                  "operation": "POST /x",
                                  "lob": "TERM",
                                  "payloadBase64": "%s"
                                }
                                """.formatted(
                                UUID.randomUUID(),
                                Base64.getEncoder().encodeToString("x".getBytes(StandardCharsets.UTF_8)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/internal/v1/raw-payloads/{payloadId}", "no-such-payload"))
                .andExpect(status().isNotFound());
    }
}
