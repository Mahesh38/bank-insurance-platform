package com.bank.insurance.onesb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bank-facing API docs for the 1SB integration adapter. See swagger-ui.html / v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oneSbIntegrationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("1SB Integration Service")
                        .description("Bank-facing API adapter for the 1SilverBullet (1SB) insurance "
                                + "platform — master data, quote, proposal, payment, and status endpoints.")
                        .version("v1"));
    }
}
