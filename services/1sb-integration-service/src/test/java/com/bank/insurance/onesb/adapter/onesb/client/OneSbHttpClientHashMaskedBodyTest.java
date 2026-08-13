package com.bank.insurance.onesb.adapter.onesb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-001 — the audit hash must never be derived from unmasked PII, for any body shape.
 * <p>
 * {@link OneSbHttpClientTest#requestHash_isHashOfMaskedBody_notPlaintextPan()} proves this over
 * the wire for a POJO body. This class covers the remaining input shapes
 * {@code hashMaskedBody} accepts — pre-serialised JSON strings, non-JSON strings, and null —
 * because a body that takes the wrong masking path would still produce a plausible-looking
 * hash, and the defect would be invisible in the audit log.
 * <p>
 * No HTTP is performed: {@code hashMaskedBody} does not touch the transport.
 */
@Tag("unit")
@Tag("COMP-002")
class OneSbHttpClientHashMaskedBodyTest {

    private static final String PAN = "ABCDE1234F";
    private static final String MOBILE = "9876543210";

    private OneSbHttpClient client;

    @BeforeEach
    void setUp() {
        client = new OneSbHttpClient(RestClient.builder().baseUrl("http://unused.invalid").build());
    }

    @Test
    void nullBody_hashesEmptyString() {
        assertThat(client.hashMaskedBody(null)).isEqualTo(client.hashMaskedBody(""));
    }

    @Test
    void jsonObjectString_isMaskedBeforeHashing() {
        String json = "{\"pan\":\"" + PAN + "\",\"mobile\":\"" + MOBILE + "\"}";

        String hash = client.hashMaskedBody(json);

        assertThat(hash).isNotEqualTo(sha256Of(json));
        assertThat(hash).isEqualTo(client.hashMaskedBody(
                "{\"pan\":\"ZZZZZ1234F\",\"mobile\":\"" + MOBILE + "\"}"));
    }

    /** A JSON array body must take the JSON path too, not the free-text path. */
    @Test
    void jsonArrayString_isMaskedBeforeHashing() {
        String json = "[{\"pan\":\"" + PAN + "\"}]";

        assertThat(client.hashMaskedBody(json)).isNotEqualTo(sha256Of(json));
    }

    /**
     * A body that is not JSON still gets pattern-masked — a PAN in a form-encoded or plain-text
     * body must not reach the hash in the clear.
     */
    @Test
    void nonJsonString_isPatternMaskedBeforeHashing() {
        String body = "pan=" + PAN + "&mobile=" + MOBILE;

        String hash = client.hashMaskedBody(body);

        assertThat(hash).isNotEqualTo(sha256Of(body));
        assertThat(hash).isEqualTo(client.hashMaskedBody("pan=ZZZZZ1234F&mobile=" + MOBILE));
    }

    /** Looks like JSON by its braces but does not parse — must fall back to text masking, not throw. */
    @Test
    void malformedJsonString_fallsBackToTextMasking_withoutThrowing() {
        String body = "{not valid json, pan=" + PAN;

        assertThat(client.hashMaskedBody(body)).isNotEqualTo(sha256Of(body));
    }

    @Test
    void mapBody_isSerialisedThenMasked() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pan", PAN);

        String hash = client.hashMaskedBody(body);

        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo(sha256Of("{\"pan\":\"" + PAN + "\"}"));
    }

    /**
     * Serialisation failure must not break the outbound call — the client falls back to
     * {@code String.valueOf}, which is still masked.
     */
    @Test
    void unserialisableBody_fallsBackToToString_withoutThrowing() {
        Object unserialisable = new Object() {
            @Override
            public String toString() {
                return "pan=" + PAN;
            }
        };

        assertThat(client.hashMaskedBody(unserialisable)).isNotBlank();
    }

    private static String sha256Of(String value) {
        // Independent restatement of the plaintext hash, so the assertions above compare against
        // "what the hash would have been without masking" rather than against the code under test.
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void objectMapperOverload_isUsedForSerialisation() {
        OneSbHttpClient withMapper = new OneSbHttpClient(
                RestClient.builder().baseUrl("http://unused.invalid").build(),
                new com.bank.insurance.onesb.adapter.onesb.error.OneSbErrorNormaliser(),
                event -> { },
                new ObjectMapper());

        assertThat(withMapper.hashMaskedBody(Map.of("pan", PAN)))
                .isEqualTo(client.hashMaskedBody(Map.of("pan", PAN)));
    }
}
