package com.bank.common.security.internal;

/** Shared inbound service-caller authentication contract (READINESS-REVIEW §3.7). */
public final class InternalCallerAuth {

  public static final String HEADER = "X-Internal-Service-Key";
  public static final String CALLER_ATTRIBUTE = "bank.internal-caller";

  /** Wildcard privilege — the caller may perform any privileged operation. */
  public static final String ALL_PRIVILEGES = "*";

  public static final String PRIVILEGE_LOGIN_ORCHESTRATE = "login.orchestrate";
  public static final String PRIVILEGE_IDENTITIES_RESOLVE = "identities.resolve";
  public static final String PRIVILEGE_IDENTITIES_PROFILE = "identities.profile";
  public static final String PRIVILEGE_AUTHORIZATION_DECIDE = "authorization.decide";
  public static final String PRIVILEGE_PARTNER_ADMIN = "partner.admin";
  public static final String PRIVILEGE_RAW_PAYLOAD_DECRYPT = "raw-payloads.decrypt";
  public static final String PRIVILEGE_AUDIT_APPEND = "audit.append";
  public static final String PRIVILEGE_AUDIT_READ = "audit.read";
  public static final String PRIVILEGE_JOBS = "jobs";
  public static final String PRIVILEGE_PAYMENT_SESSIONS = "payment-sessions";
  public static final String PRIVILEGE_RAW_PAYLOAD_WRITE = "raw-payloads.write";

  private InternalCallerAuth() {}
}
