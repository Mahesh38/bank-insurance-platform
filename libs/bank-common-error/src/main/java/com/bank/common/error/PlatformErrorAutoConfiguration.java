package com.bank.common.error;

import com.bank.common.observability.ErrorMetrics;
import com.bank.common.observability.RequestDiagnosticFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Wires the error contract into a service that does nothing but depend on this library.
 *
 * <p>Before this class, adopting the contract meant hand-writing a {@code RequestDiagnosticConfig}
 * (twenty-six lines, byte-identical across five services apart from one string) and an advice
 * subclass, and repeating the service id as a literal. That is boilerplate a library should absorb:
 * a service now declares what is true about it in {@code application.yml} and gets the behaviour.
 *
 * <p>Every bean is {@link ConditionalOnMissingBean}, so a service with genuinely service-specific
 * needs — the BFF's authentication handlers — supplies its own and this steps aside.
 */
@AutoConfiguration
@EnableConfigurationProperties(PlatformErrorProperties.class)
public class PlatformErrorAutoConfiguration {

    /**
     * @throws IllegalStateException when diagnostics are exposed under a production profile. A
     *         misconfigured switch here puts upstream prose and internal routes on a device, so it
     *         fails at startup rather than at the first error — the difference between an incident
     *         found by a deployment and one found by a customer.
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorHandlingSettings errorHandlingSettings(PlatformErrorProperties properties,
                                                       Environment environment) {
        boolean production = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

        if (properties.isExposeDiagnostics() && production) {
            throw new IllegalStateException(
                "bank.error.expose-diagnostics is true under a production profile. Diagnostics "
                    + "carry upstream prose, internal routes and cause chains; they are for logs "
                    + "and internal hops, never for a device. Use the incidentId to retrieve them.");
        }

        return ErrorHandlingSettings.builder(resolveServiceId(properties, environment))
            .layer(properties.getLayer())
            .boundary(properties.getBoundary())
            .exposeDiagnostics(properties.isExposeDiagnostics())
            .validationStatus(properties.getValidationStatus())
            .malformedBodyStatusOverride(properties.getMalformedBodyStatus())
            .build();
    }

    /** Injected wherever a service throws, so no throw site carries the service id as a literal. */
    @Bean
    @ConditionalOnMissingBean
    public ServiceErrors serviceErrors(ErrorHandlingSettings settings) {
        return new ServiceErrors(settings);
    }

    /** Reads a peer's error envelope so {@link ErrorPropagation} has something to propagate. */
    @Bean
    @ConditionalOnMissingBean
    public ProblemJsonReader problemJsonReader(
            ObjectProvider<com.fasterxml.jackson.databind.ObjectMapper> objectMapper) {
        return new ProblemJsonReader(objectMapper.getIfAvailable(
            com.fasterxml.jackson.databind.ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorRecorder errorRecorder(ErrorHandlingSettings settings,
                                       PlatformErrorProperties properties,
                                       ObjectProvider<MeterRegistry> meterRegistry) {
        // A @WebMvcTest slice has no MeterRegistry, and a service must not lose its error
        // responses because it cannot count them.
        MeterRegistry registry = properties.isMetricsEnabled() ? meterRegistry.getIfAvailable() : null;
        return new Slf4jErrorRecorder(settings.layer(), registry != null ? new ErrorMetrics(registry) : null);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformErrorHandler.class)
    public PlatformErrorAdvice platformErrorAdvice(ErrorHandlingSettings settings,
                                                   ErrorRecorder recorder) {
        return new PlatformErrorAdvice(settings, recorder);
    }

    /** Seeds correlation context at the edge, before any other filter can log or reject. */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<RequestDiagnosticFilter> requestDiagnosticFilter(
            ErrorHandlingSettings settings) {
        FilterRegistrationBean<RequestDiagnosticFilter> registration =
            new FilterRegistrationBean<>(new RequestDiagnosticFilter(settings.serviceId()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    private static String resolveServiceId(PlatformErrorProperties properties, Environment environment) {
        if (StringUtils.hasText(properties.getServiceId())) {
            return properties.getServiceId();
        }
        String applicationName = environment.getProperty("spring.application.name");
        if (StringUtils.hasText(applicationName)) {
            return applicationName;
        }
        throw new IllegalStateException(
            "No service id: set bank.error.service-id or spring.application.name. Every error "
                + "response, log line and metric tag names the service that produced it, and an "
                + "unattributed error is the problem this contract exists to remove.");
    }
}
