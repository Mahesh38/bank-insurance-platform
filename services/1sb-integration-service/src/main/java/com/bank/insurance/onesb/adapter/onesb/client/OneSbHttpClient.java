package com.bank.insurance.onesb.adapter.onesb.client;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Single HTTP stack for all outbound 1SB calls (Basic Auth, timeouts, no 401 retry).
 * Controllers and application services must not call this directly — use domain ports.
 */
@Component
public class OneSbHttpClient {

    private static final Logger log = LoggerFactory.getLogger(OneSbHttpClient.class);

    private final RestClient restClient;

    public OneSbHttpClient(@Qualifier("oneSbRestClient") RestClient oneSbRestClient) {
        this.restClient = oneSbRestClient;
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(HttpMethod.POST, path, body, responseType);
    }

    public <T> T exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        log.debug("1SB {} {}", method, path);
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(path);
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.retrieve()
                    .onStatus(status -> status.value() == 401, (request, response) -> {
                        // Explicit: never retry 401
                        throw ServiceException.upstreamAuth("1SB returned 401 Unauthorized");
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw mapHttpError(response.getStatusCode().value(), readBody(response));
                    })
                    .body(responseType);
        } catch (ServiceException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                throw ServiceException.upstreamAuth("1SB returned 401 Unauthorized");
            }
            throw mapHttpError(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            if (ex.getCause() instanceof ServiceException se) {
                throw se;
            }
            throw ServiceException.upstreamUnavailable("1SB call failed: " + method + " " + path, ex);
        }
    }

    private static ServiceException mapHttpError(int status, String responseBody) {
        if (status == 401) {
            return ServiceException.upstreamAuth("1SB returned 401 Unauthorized");
        }
        if (status >= 500) {
            return ServiceException.upstreamUnavailable(
                    "1SB returned " + status, null);
        }
        if (status >= 400) {
            return ServiceException.upstreamBusiness(
                    "1SB returned " + status,
                    null);
        }
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Upstream Bad Response")
                .status(502)
                .detail("Unexpected 1SB status " + status)
                .code(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .retryable(false)
                .build());
    }

    private static String readBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bytes = response.getBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
