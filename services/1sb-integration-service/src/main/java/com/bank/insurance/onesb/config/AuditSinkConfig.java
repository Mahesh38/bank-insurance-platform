package com.bank.insurance.onesb.config;

import com.bank.common.audit.AuditEventPublisher;
import com.bank.insurance.onesb.adapter.persistence.HttpAuditEventStoreAdapter;
import com.bank.insurance.onesb.observability.CompositeAuditEventPublisher;
import com.bank.insurance.onesb.observability.LoggingAuditEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the audit sinks named by {@link AuditProperties} (RISK-012).
 *
 * <p>Service code depends only on {@link AuditEventPublisher}. Which transports are behind it —
 * log, HTTP to the persistence service, or an event backbone later — is decided here, from
 * configuration. That is what lets the production transport be chosen after the fact without
 * touching a single call site.
 *
 * <p><b>KAFKA fails startup rather than degrading.</b> It is a declared but unimplemented sink.
 * If a future environment selects it, the service refuses to boot with an explicit message. The
 * alternative — accepting the value and quietly publishing nowhere — would produce a deployment
 * that looks healthy while silently discarding compliance evidence. This mirrors the fail-fast
 * already used for the AWS Secrets Manager stub (TD-006).
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditSinkConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditSinkConfig.class);

    @Bean
    AuditEventPublisher auditEventPublisher(
            AuditProperties properties,
            @Qualifier("persistenceRestClient") RestClient persistenceRestClient,
            ObjectMapper objectMapper) {

        List<AuditProperties.Sink> selected = properties.resolvedSinks();
        if (selected.isEmpty()) {
            throw new IllegalStateException(
                    "insurance.audit.sinks resolved to nothing. Audit events would be discarded "
                            + "silently — refusing to start. Set at least one of LOG, PERSISTENCE.");
        }

        List<AuditEventPublisher> sinks = new ArrayList<>();
        for (AuditProperties.Sink sink : selected) {
            switch (sink) {
                case LOG -> sinks.add(new LoggingAuditEventPublisher());
                case PERSISTENCE -> sinks.add(
                        new HttpAuditEventStoreAdapter(persistenceRestClient, objectMapper));
                case KAFKA -> throw new IllegalStateException(
                        "Audit sink KAFKA is declared but not implemented. Selecting it would "
                                + "discard every audit event while appearing healthy — refusing "
                                + "to start. Implement an AuditEventPublisher for it and register "
                                + "it here, or use PERSISTENCE.");
            }
        }

        log.info("Audit sinks active: {}", selected);
        return new CompositeAuditEventPublisher(sinks);
    }
}
