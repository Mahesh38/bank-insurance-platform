package com.bank.insurance.onesb.adapter.onesb.client;

import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.error.OneSbErrorNormaliser;
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
    private final OneSbErrorNormaliser errorNormaliser;

    @org.springframework.beans.factory.annotation.Autowired
    public OneSbHttpClient(
            @Qualifier("oneSbRestClient") RestClient oneSbRestClient,
            OneSbErrorNormaliser errorNormaliser) {
        this.restClient = oneSbRestClient;
        this.errorNormaliser = errorNormaliser;
    }

    /** Convenience for tests / temporary poll adapters. */
    public OneSbHttpClient(RestClient oneSbRestClient) {
        this(oneSbRestClient, new OneSbErrorNormaliser());
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
                        throw errorNormaliser.normalise(401, readBody(response));
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw errorNormaliser.normalise(
                                response.getStatusCode().value(), readBody(response));
                    })
                    .body(responseType);
        } catch (ServiceException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw errorNormaliser.normalise(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            if (ex.getCause() instanceof ServiceException se) {
                throw se;
            }
            throw ServiceException.upstreamUnavailable("1SB call failed: " + method + " " + path, ex);
        }
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
