package com.bank.persistence.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Internal API docs — {@code bank-persistence-service} is not public; this is for the
 * platform microservices (1sb-integration-service, future audit-consumer-service) that
 * call {@code /internal/v1/*} over the private network. See swagger-ui.html / v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI persistenceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Persistence Service — Internal API")
                        .description("Platform-common job/offer/payment-session/audit-event/raw-payload "
                                + "store, exposed to internal microservices only (not a public bank API).")
                        .version("v1"));
    }
}
