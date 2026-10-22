package com.bank.platform.consent.config;

import com.bank.common.error.AuditDisposition;
import com.bank.common.error.ErrorCategory;
import com.bank.common.error.ErrorDefinition;
import com.bank.common.error.ErrorDefinitionProvider;
import com.bank.common.error.Propagation;
import com.bank.common.error.Retryability;
import com.bank.platform.consent.domain.ConsentCodes;
import java.util.Collection;
import java.util.List;

/**
 * Consent refusals that are not already in the platform catalogue. Additive only: codes such as
 * {@code CONSENT_REQUIRED} stay owned by the shared catalogue.
 */
public final class ConsentErrorCatalogue implements ErrorDefinitionProvider {

  @Override
  public Collection<ErrorDefinition> definitions() {
    return List.of(
        def(
            ConsentCodes.CONSENT_EXPIRED,
            ErrorCategory.COMPLIANCE_GATE,
            403,
            Retryability.AFTER_REMEDIATION,
            "Consent expired",
            "This consent is no longer within its validity window.",
            AuditDisposition.COMPLIANCE_EVENT),
        def(
            ConsentCodes.CONSENT_SCOPE_MISMATCH,
            ErrorCategory.COMPLIANCE_GATE,
            403,
            Retryability.AFTER_REMEDIATION,
            "Consent scope mismatch",
            "This consent does not cover the requested insurer.",
            AuditDisposition.COMPLIANCE_EVENT),
        def(
            ConsentCodes.CONSENT_SUBJECT_MISMATCH,
            ErrorCategory.COMPLIANCE_GATE,
            403,
            Retryability.NO,
            "Consent subject mismatch",
            "The consent does not belong to this customer.",
            AuditDisposition.SECURITY_EVENT),
        def(
            ConsentCodes.CONSENT_OTP_REQUIRED,
            ErrorCategory.CONFLICT,
            409,
            Retryability.AFTER_FIX,
            "Consent code required",
            "Enter the code sent to the registered contact.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.CONSENT_OTP_NOT_VERIFIED,
            ErrorCategory.CONFLICT,
            409,
            Retryability.AFTER_FIX,
            "Consent code not verified",
            "The code did not match.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.OTP_EXPIRED,
            ErrorCategory.CONFLICT,
            409,
            Retryability.AFTER_REMEDIATION,
            "Code expired",
            "The code window has elapsed. Request a new code.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.OTP_ATTEMPTS_EXCEEDED,
            ErrorCategory.RATE_LIMIT,
            429,
            Retryability.YES,
            "Too many code attempts",
            "This code is locked. Request a new code later.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.OTP_ISSUE_RATE_EXCEEDED,
            ErrorCategory.RATE_LIMIT,
            429,
            Retryability.YES,
            "Too many code requests",
            "The code request limit has been reached.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED,
            ErrorCategory.INTERNAL,
            500,
            Retryability.NO,
            "Consent could not be recorded",
            "The consent could not be stored. Nothing was granted.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.STATEMENT_VERSION_IMMUTABLE,
            ErrorCategory.CONFLICT,
            409,
            Retryability.NO,
            "Statement version exists",
            "That statement version is already published.",
            AuditDisposition.NONE),
        def(
            ConsentCodes.JOURNEY_HALTED_CONSENT_WITHDRAWN,
            ErrorCategory.COMPLIANCE_GATE,
            403,
            Retryability.AFTER_REMEDIATION,
            "Journey halted",
            "This consent was withdrawn. The journey cannot continue.",
            AuditDisposition.COMPLIANCE_EVENT),
        def(
            ConsentCodes.CONSENT_DEVICE_ISOLATION,
            ErrorCategory.COMPLIANCE_GATE,
            403,
            Retryability.NO,
            "Customer device required",
            "This code can only be confirmed on the customer device.",
            AuditDisposition.COMPLIANCE_EVENT),
        def(
            ConsentCodes.STATEMENT_NOT_PUBLISHED,
            ErrorCategory.VALIDATION,
            422,
            Retryability.AFTER_FIX,
            "Statement not published",
            "Publish the statement version before capturing consent.",
            AuditDisposition.NONE));
  }

  private static ErrorDefinition def(
      String code,
      ErrorCategory category,
      int status,
      Retryability retryability,
      String title,
      String detail,
      AuditDisposition audit) {
    return new ErrorDefinition(
        code,
        category,
        status,
        retryability,
        title,
        detail,
        audit,
        Propagation.PROPAGATE,
        null,
        "consent-rule-pack");
  }
}
