package com.bank.insurance.onesb.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WS-1 Phase 4 exit criterion <b>4.2</b> — keeps the published OpenAPI document honest.
 *
 * <p>springdoc generates the specification at runtime, which means the document a bank consumer
 * integrates against lives only inside a running process. Publishing a snapshot of it solves
 * that, and immediately creates the usual problem: the snapshot goes stale the first time
 * somebody adds a field, and nobody notices until a consumer's generated client breaks.
 *
 * <p>So the snapshot is committed <em>and</em> verified. This test fails when the code and the
 * published document disagree, which makes the document a contract rather than a souvenir.
 *
 * <p>To accept an intentional API change:
 * <pre>{@code
 * ./gradlew :services:1sb-integration-service:test --tests '*OpenApiContractTest' \
 *     -DupdateOpenApi=true
 * }</pre>
 * then commit the regenerated file. Treat a diff in this file as an API review item — for a
 * breaking change that is an Architecture verdict
 * (00-GOVERNANCE section 8, non-negotiable 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Tag("FUNC-API")
class OpenApiContractTest {

    /** Repo-relative; resolved against the repository root, not the module or the CWD. */
    private static final String PUBLISHED_SPEC =
            "docs/1sb-insurance-integration/api-catalog/openapi/1sb-integration-service.json";

    private static final String CONSUMER_COLLECTION =
            "docs/1sb-insurance-integration/api-catalog/collections/"
                    + "1sb-integration-term-journey.postman_collection.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedSpecMatchesTheRunningApi() throws Exception {
        String generated = normalise(fetchSpec());
        Path published = repoRoot().resolve(PUBLISHED_SPEC);

        if (Boolean.getBoolean("updateOpenApi")) {
            Files.createDirectories(published.getParent());
            Files.writeString(published, generated + System.lineSeparator(), StandardCharsets.UTF_8);
            return;
        }

        assertThat(published)
                .as("published OpenAPI document is missing — regenerate with -DupdateOpenApi=true")
                .exists();

        assertThat(normalise(MAPPER.readTree(Files.readString(published, StandardCharsets.UTF_8))))
                .as("""
                        The published OpenAPI document no longer matches the running API.

                        If the API change is intentional, regenerate and commit:
                          ./gradlew :services:1sb-integration-service:test \\
                              --tests '*OpenApiContractTest' -DupdateOpenApi=true

                        If it is not intentional, the API changed by accident — that is the bug.
                        """)
                .isEqualTo(generated);
    }

    /** The bank-facing surface a consumer is entitled to rely on. */
    @Test
    void specDocumentsEveryBankFacingEndpoint() throws Exception {
        JsonNode paths = fetchSpec().get("paths");

        assertThat(paths).isNotNull();
        assertThat(paths.fieldNames()).toIterable().contains(
                "/v1/quotes",
                "/v1/quotes/{jobId}",
                "/v1/proposals",
                "/v1/proposals/schema",
                "/v1/proposals/{jobId}",
                "/v1/payments",
                "/v1/status/{applicationNumber}",
                "/v1/master-data/lookup");
    }

    /**
     * The internal persistence API must never appear in the bank-facing document. Publishing it
     * would advertise a route bank apps are forbidden to call (standing constraint: bank apps
     * never reach the database directly).
     */
    @Test
    void specDoesNotLeakInternalRoutes() throws Exception {
        JsonNode paths = fetchSpec().get("paths");

        assertThat(paths.fieldNames()).toIterable()
                .as("internal routes must not be published to bank consumers")
                .noneMatch(path -> path.startsWith("/internal/"));
    }

    /**
     * The consumer collection is the first thing a bank integrator runs. If it calls a route the
     * service no longer exposes, their first experience of the integration is a 404 — and nobody
     * would find out until then, because a JSON collection has nothing else checking it.
     */
    @Test
    void consumerCollectionOnlyCallsPublishedRoutes() throws Exception {
        JsonNode spec = fetchSpec();
        JsonNode collection = MAPPER.readTree(
                Files.readString(repoRoot().resolve(CONSUMER_COLLECTION), StandardCharsets.UTF_8));

        java.util.List<String> specPaths = new java.util.ArrayList<>();
        spec.get("paths").fieldNames().forEachRemaining(specPaths::add);

        java.util.List<String> called = new java.util.ArrayList<>();
        for (JsonNode item : collection.get("item")) {
            JsonNode path = item.path("request").path("url").path("path");
            if (!path.isArray()) {
                continue;
            }
            StringBuilder route = new StringBuilder();
            for (JsonNode segment : path) {
                route.append('/').append(toSpecSegment(segment.asText()));
            }
            called.add(route.toString());
        }

        assertThat(called).as("collection contains no requests — it cannot have been parsed")
                .isNotEmpty();
        assertThat(specPaths)
                .as("consumer collection calls routes the published API does not expose")
                .containsAll(called.stream().distinct().toList());
    }

    /**
     * Postman variables are the collection's way of writing a path parameter, so
     * {@code {{jobId}}} in the collection is {@code {jobId}} in the specification. Mapping by
     * position rather than by name keeps the two from having to agree on variable naming.
     */
    private static String toSpecSegment(String segment) {
        if (!segment.startsWith("{{") || !segment.endsWith("}}")) {
            return segment;
        }
        String name = segment.substring(2, segment.length() - 2);
        return switch (name) {
            case "jobId", "proposalJobId" -> "{jobId}";
            case "applicationNumber" -> "{applicationNumber}";
            default -> segment;
        };
    }

    private JsonNode fetchSpec() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return MAPPER.readTree(body);
    }

    /**
     * springdoc emits a {@code servers} block derived from the request, so the document differs
     * between a MockMvc run and a real host. Dropping it keeps the published document stable and
     * environment-neutral — the base URL belongs in the consumer's configuration anyway.
     */
    private static String normalise(JsonNode spec) throws IOException {
        JsonNode copy = spec.deepCopy();
        if (copy.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) copy).remove("servers");
        }
        return PRETTY.writeValueAsString(MAPPER.readTree(MAPPER.writeValueAsString(copy)));
    }

    /** Walks up from the working directory to the directory holding settings.gradle.kts. */
    private static Path repoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("repository root (settings.gradle.kts) not found");
        }
        return dir;
    }
}
