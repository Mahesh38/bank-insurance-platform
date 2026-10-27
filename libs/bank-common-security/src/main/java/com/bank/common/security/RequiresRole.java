package com.bank.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation declaring required role(s).
 *
 * <p><strong>Not a control by itself.</strong> This annotation is a contract only — there is no
 * platform AOP aspect that enforces it. Business authorization is enforced by the identity-
 * authorization PDP and by {@code InternalCallerAuth} on east-west APIs (SUG-20261027-iah). Do not
 * treat presence of {@code @RequiresRole} as evidence that a route is authorized.
 *
 * <p>Example (documentation only until a service registers an enforcer):
 *
 * <pre>{@code
 * @RequiresRole(Role.RM)
 * public QuoteResponse createQuote(QuoteRequest req) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
  /** One or more roles required; principal must hold AT LEAST ONE. */
  Role[] value();
}
