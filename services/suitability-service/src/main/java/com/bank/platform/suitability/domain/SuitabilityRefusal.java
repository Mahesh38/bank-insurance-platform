package com.bank.platform.suitability.domain;

import com.bank.platform.suitability.domain.JourneyModel.FieldError;

/** A journey refusal. The message is the seller-facing BRD sentence and carries no contact data. */
public final class SuitabilityRefusal extends RuntimeException {

  private final String code;
  private final String valId;
  private final String field;

  public SuitabilityRefusal(String code, String message) {
    this(code, null, null, message);
  }

  public SuitabilityRefusal(String code, String valId, String field, String message) {
    super(message);
    this.code = code;
    this.valId = valId;
    this.field = field;
  }

  public static SuitabilityRefusal validation(FieldError error) {
    return new SuitabilityRefusal("VALIDATION_ERROR", error.id(), error.field(), error.message());
  }

  public String code() {
    return code;
  }

  public String valId() {
    return valId;
  }

  public String field() {
    return field;
  }
}
