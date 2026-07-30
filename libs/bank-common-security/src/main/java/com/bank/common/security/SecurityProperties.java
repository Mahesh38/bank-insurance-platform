package com.bank.common.security;

/**
 * Configuration properties for JWT validation.
 *
 * <p>Concrete implementations should be annotated with
 * {@code @ConfigurationProperties("bank.security.jwt")} in the consuming service.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * bank:
 *   security:
 *     jwt:
 *       issuer: https://auth.bank.internal
 *       audience: insurance-service
 *       jwks-uri: https://auth.bank.internal/.well-known/jwks.json
 * </pre>
 */
public interface SecurityProperties {

    /** JWT {@code iss} claim — must match exactly. */
    String getIssuer();

    /** JWT {@code aud} claim this service expects. */
    String getAudience();

    /** JWKS endpoint URL used to verify JWT signatures. */
    String getJwksUri();
}
