package com.bank.identity.provider.adapter.adverify;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.identity.provider.config.BankAdVerifyProperties;
import com.bank.identity.provider.config.BankAdVerifyProperties.StubUser;
import com.bank.identity.provider.domain.WorkforceCredentialVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the bank AD-verify API (true/false for an active bank employee) or a local stub.
 * Passwords are never logged or retained.
 */
@Component
public class BankAdVerifyAdapter implements WorkforceCredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(BankAdVerifyAdapter.class);

    private final BankAdVerifyProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ServiceErrors errors;
    private final Environment environment;

    public BankAdVerifyAdapter(
        BankAdVerifyProperties properties,
        @Qualifier("adVerifyRestClient") RestClient restClient,
        ObjectMapper objectMapper,
        ServiceErrors errors,
        Environment environment
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.errors = errors;
        this.environment = environment;
    }

    @PostConstruct
    void rejectStubInProduction() {
        if (environment.matchesProfiles("prod") && properties.stubMode()) {
            throw new IllegalStateException("identity.ad-verify.mode=stub is forbidden under the prod profile");
        }
        if (!properties.stubMode()
            && (properties.http() == null || properties.http().baseUrl() == null)) {
            throw new IllegalStateException("identity.ad-verify.http.base-url is required when mode=http");
        }
    }

    @Override
    public AdVerifyResult verify(AdVerifyCommand command) {
        requireIdentifier(command);
        if (properties.stubMode()) {
            return verifyStub(command);
        }
        return verifyHttp(command);
    }

    private AdVerifyResult verifyStub(AdVerifyCommand command) {
        StubUser match = properties.stubUsers().stream()
            .filter(user -> constantEquals(user.employeeId(), command.employeeId()))
            .findFirst()
            .orElse(null);
        if (match == null || !constantEquals(match.password(), command.password())) {
            log.info("AD-verify stub rejected credentials");
            return rejected(command.employeeId());
        }
        if (!match.active()) {
            log.info("AD-verify stub found an inactive directory account");
            return new AdVerifyResult(false, false, command.employeeId(), null, null);
        }
        log.info("AD-verify stub accepted an active directory account");
        return new AdVerifyResult(
            true,
            true,
            command.employeeId(),
            hasText(match.username()) ? match.username() : command.employeeId(),
            match.email()
        );
    }

    private AdVerifyResult verifyHttp(AdVerifyCommand command) {
        URI uri = verifyUri();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("employeeId", command.employeeId());
        body.put("password", command.password());
        try {
            var request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
            String apiKey = properties.http().apiKey();
            if (hasText(apiKey)) {
                request = request.header(properties.http().apiKeyHeaderName(), apiKey);
            }
            ResponseEntity<String> response = request.retrieve().toEntity(String.class);
            return parseBankResponse(command.employeeId(), response.getBody());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                log.info("AD-verify HTTP rejected credentials status={}", status);
                return rejected(command.employeeId());
            }
            throw errors.error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
                .component("BankAdVerifyAdapter")
                .operation("verify")
                .reason("AD-verify HTTP status " + status)
                .cause(exception)
                .build();
        } catch (RuntimeException exception) {
            throw errors.error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
                .component("BankAdVerifyAdapter")
                .operation("verify")
                .reason("AD-verify HTTP call failed")
                .cause(exception)
                .build();
        }
    }

    private AdVerifyResult parseBankResponse(String employeeId, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return rejected(employeeId);
        }
        String trimmed = rawBody.trim();
        try {
            if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                boolean authenticated = Boolean.parseBoolean(trimmed);
                return authenticated
                    ? accepted(employeeId, employeeId, null)
                    : rejected(employeeId);
            }
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isBoolean()) {
                return node.booleanValue()
                    ? accepted(employeeId, employeeId, null)
                    : rejected(employeeId);
            }
            if (node.isTextual()
                && ("true".equalsIgnoreCase(node.asText()) || "false".equalsIgnoreCase(node.asText()))) {
                return node.asBoolean()
                    ? accepted(employeeId, employeeId, null)
                    : rejected(employeeId);
            }
            boolean authenticated = booleanField(node, "authenticated", "success", "isAuthenticated");
            boolean accountActive = node.has("accountActive")
                ? node.get("accountActive").asBoolean()
                : (!node.has("active") || node.get("active").asBoolean(authenticated));
            if (!authenticated) {
                return rejected(employeeId);
            }
            String username = textField(node, "username", "userId", "employeeId");
            String email = textField(node, "email");
            return new AdVerifyResult(
                true,
                accountActive,
                employeeId,
                hasText(username) ? username : employeeId,
                email
            );
        } catch (Exception exception) {
            throw errors.error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
                .component("BankAdVerifyAdapter")
                .operation("parseBankResponse")
                .reason("AD-verify response was not a boolean payload")
                .cause(exception)
                .build();
        }
    }

    private URI verifyUri() {
        String base = properties.http().baseUrl().toString().replaceAll("/$", "");
        String path = properties.http().path().startsWith("/")
            ? properties.http().path()
            : "/" + properties.http().path();
        return URI.create(base + path);
    }

    private static AdVerifyResult accepted(String employeeId, String username, String email) {
        return new AdVerifyResult(true, true, employeeId, username, email);
    }

    private static AdVerifyResult rejected(String employeeId) {
        return new AdVerifyResult(false, false, employeeId, null, null);
    }

    private void requireIdentifier(AdVerifyCommand command) {
        if (command == null || !hasText(command.employeeId()) || command.password() == null) {
            throw errors.error(ErrorCodes.SCHEMA_INVALID)
                .component("BankAdVerifyAdapter")
                .operation("verify")
                .reason("employeeId and password are required")
                .build();
        }
    }

    private static boolean booleanField(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name)) {
                JsonNode value = node.get(name);
                if (value.isBoolean()) {
                    return value.booleanValue();
                }
                if (value.isTextual()) {
                    return Boolean.parseBoolean(value.asText());
                }
            }
        }
        return false;
    }

    private static String textField(JsonNode node, String... names) {
        for (String name : names) {
            if (node.hasNonNull(name) && node.get(name).isTextual()) {
                String value = node.get(name).asText();
                if (hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean constantEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
            left.getBytes(StandardCharsets.UTF_8),
            right.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
