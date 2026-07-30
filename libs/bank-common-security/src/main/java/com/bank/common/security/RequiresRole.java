package com.bank.common.security;

import java.lang.annotation.*;

/**
 * Method-level annotation declaring required role(s).
 *
 * <p>Consuming services must register an AOP aspect or Spring Security
 * {@code @PreAuthorize} equivalent that enforces this annotation.
 * The lib itself only defines the contract — not the enforcement mechanism —
 * so that services can choose their AOP strategy.
 *
 * <p>Example:
 * <pre>{@code
 * @RequiresRole(Role.RM)
 * public QuoteResponse createQuote(QuoteRequest req) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    /**
     * One or more roles required; principal must hold AT LEAST ONE.
     */
    Role[] value();
}
