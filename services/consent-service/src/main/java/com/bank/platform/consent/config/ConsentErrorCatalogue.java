package com.bank.platform.consent.config;

import com.bank.common.error.AuditDisposition;
import com.bank.common.error.ErrorCategory;
import com.bank.common.error.ErrorDefinition;
import com.bank.common.error.ErrorDefinitionProvider;
import com.bank.common.error.Propagation;
import com.bank.common.error.Retryability;
import java.util.Collection;
import java.util.List;

/**
 * Codes the shared catalogue does not own. Platform codes such as {@code OTP_EXPIRED} and {@code
 * CONSENT_SUBJECT_MISMATCH} stay in {@code bank-common-error}.
 */
public final class ConsentErrorCatalogue implements ErrorDefinitionProvider {

  public static final String CONSENT_EVIDENCE_WRITE_FAILED = "CONSENT_EVIDENCE_WRITE_FAILED";

  @Override
  public Collection<ErrorDefinition> definitions() {
    return List.of(
        new ErrorDefinition(
            CONSENT_EVIDENCE_WRITE_FAILED,
            ErrorCategory.INTERNAL,
            503,
            Retryability.NO,
            "Consent could not be recorded",
            "The consent could not be stored. Nothing was granted.",
            AuditDisposition.NONE,
            Propagation.PROPAGATE,
            null,
            "consent-service"));
  }
}
