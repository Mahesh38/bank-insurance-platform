package com.bank.insurance.onesb.application.validation;

import com.bank.common.domain.Lob;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Fail-before-1SB Life quote checks for Term, Saving and ULIP (FUNC-028). Closed enums come from
 * {@link LifeContractCatalog}; product-specific lists stay out.
 */
public final class LifeQuoteValidator {

  private static final DateTimeFormatter ISO_DATE =
      DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
  private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");
  private static final int MIN_AGE = 18;
  private static final int MAX_AGE = 75;
  private static final int MIN_TERM = 1;
  private static final int MAX_TERM = 80;

  private LifeQuoteValidator() {}

  public static List<ServiceError> validate(CreateQuoteCommand command) {
    List<ServiceError> errors = new ArrayList<>();
    if (command == null) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD, "quote command is required", "body"));
      return errors;
    }
    if (command.lob() == null) {
      errors.add(ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD, "lob is required", "lob"));
    } else if (!isLife(command.lob())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.UNSUPPORTED_LOB,
              "Unsupported lob for Life quote: " + command.lob(),
              "lob"));
    }
    if (!LifeContractCatalog.isKnownMode(command.mode())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR, "mode must be SINGLE or MULTI", "mode"));
    }
    if (!LifeContractCatalog.isKnownCategory(command.category())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "category must be PREMIUM, SUM_ASSURED or INCOME",
              "category"));
    }
    validateMoney(command, errors);
    validateMembers(command, errors);
    validateDistribution(command, errors);
    validateSelection(command, errors);
    validateSavingsFilter(command, errors);
    return List.copyOf(errors);
  }

  public static boolean isLife(Lob lob) {
    return lob == Lob.TERM || lob == Lob.SAVING || lob == Lob.ULIP;
  }

  private static void validateMoney(CreateQuoteCommand command, List<ServiceError> errors) {
    if (command.sumAssured() == null) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD, "sumAssured is required", "sumAssured"));
    } else if (command.sumAssured().signum() <= 0) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR, "sumAssured must be greater than zero", "sumAssured"));
    }
    if (command.premiumAmount() != null && command.premiumAmount().signum() <= 0) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "premiumAmount must be greater than zero",
              "premiumAmount"));
    }
    String category = command.category() == null ? "" : command.category().trim().toUpperCase();
    if ("PREMIUM".equals(category) && command.premiumAmount() == null) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD,
              "premiumAmount is required when category is PREMIUM",
              "premiumAmount"));
    }
  }

  private static void validateMembers(CreateQuoteCommand command, List<ServiceError> errors) {
    if (command.members() == null || command.members().isEmpty()) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD, "members must be non-empty", "members"));
      return;
    }
    boolean hasLifeAssured = false;
    boolean hasProposer = false;
    for (int i = 0; i < command.members().size(); i++) {
      CreateQuoteCommand.MemberDetail m = command.members().get(i);
      String prefix = "members[" + i + "]";
      if (m.role() != null
          && !m.role().isBlank()
          && !LifeContractCatalog.isKnownMemberType(m.role())) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "unsupported member role; use LIFE_ASSURED or PROPOSER",
                prefix + ".role"));
      }
      String roleKey = m.role() == null ? "LIFE_ASSURED" : m.role().trim().toUpperCase();
      if (roleKey.contains("PROPOSER")) {
        hasProposer = true;
      } else {
        hasLifeAssured = true;
      }
      rejectBlank(errors, m.dob(), prefix + ".dob", "dob is required");
      rejectBlank(errors, m.gender(), prefix + ".gender", "gender is required");
      if (StringUtils.hasText(m.dob())) {
        validateDob(m.dob().trim(), prefix + ".dob", errors);
      }
      if (StringUtils.hasText(m.gender()) && !LifeContractCatalog.isKnownGender(m.gender())) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "gender must be Male, Female, Transgender or Others (or M/F)",
                prefix + ".gender"));
      }
      if (m.annualIncome() == null) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD,
                "annualIncome is required",
                prefix + ".annualIncome"));
      } else if (m.annualIncome().signum() <= 0) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "annualIncome must be greater than zero",
                prefix + ".annualIncome"));
      }
      if (!StringUtils.hasText(m.pincode())) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD, "pincode is required", prefix + ".pincode"));
      } else if (!PINCODE.matcher(m.pincode().trim()).matches()) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "pincode must be a 6-digit Indian postal code",
                prefix + ".pincode"));
      }
      if (isSingleQuote(command.mode())
          && hasProposer
          && roleKey.contains("PROPOSER")
          && !StringUtils.hasText(m.relationship())) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD,
                "relationship is required on the proposer for Single Quote",
                prefix + ".relationship"));
      }
    }
    if (!hasLifeAssured) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR, "at least one life assured is required", "members"));
    }
  }

  private static void validateDistribution(CreateQuoteCommand command, List<ServiceError> errors) {
    CreateQuoteCommand.DistributionContext dist = command.distribution();
    String agent = dist == null ? null : firstText(dist.agentId(), dist.rmEmployeeId());
    if (!StringUtils.hasText(agent)) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD,
              "distribution.agentId is required",
              "distribution.agentId"));
    }
    if (dist != null && !LifeContractCatalog.isKnownChannel(dist.channelType())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "channelType must be B2B or B2C",
              "distribution.channelType"));
    }
  }

  private static void validateSelection(CreateQuoteCommand command, List<ServiceError> errors) {
    if (isSingleQuote(command.mode())) {
      CreateQuoteCommand.ProductSelection selection = command.selection();
      if (selection == null || !StringUtils.hasText(selection.insurerCode())) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD,
                "selection.insurerCode is required for Single Quote",
                "selection.insurerCode"));
      }
      if (selection == null
          || selection.productCodes() == null
          || selection.productCodes().stream().noneMatch(StringUtils::hasText)) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD,
                "selection.productCodes is required for Single Quote",
                "selection.productCodes"));
      }
    }
    CreateQuoteCommand.ProductSelection selection = command.selection();
    if (selection == null) {
      return;
    }
    validateTerm(selection.policyTerm(), "selection.policyTerm", errors);
    validateTerm(selection.premiumPaymentTerm(), "selection.premiumPaymentTerm", errors);
    if (selection.policyTerm() != null
        && selection.premiumPaymentTerm() != null
        && selection.premiumPaymentTerm() > selection.policyTerm()) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "premiumPaymentTerm cannot exceed policyTerm",
              "selection.premiumPaymentTerm"));
    }
    if (!LifeContractCatalog.isKnownFrequency(selection.premiumFrequency())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "premiumFrequency must be M, Q, HY, Y or S",
              "selection.premiumFrequency"));
    }
  }

  private static void validateTerm(Integer value, String field, List<ServiceError> errors) {
    if (value == null) {
      return;
    }
    if (value < MIN_TERM || value > MAX_TERM) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              field + " must be between " + MIN_TERM + " and " + MAX_TERM,
              field));
    }
  }

  private static void validateDob(String dob, String field, List<ServiceError> errors) {
    try {
      LocalDate date = LocalDate.parse(dob, ISO_DATE);
      LocalDate today = LocalDate.now();
      if (date.isAfter(today)) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR, "dob cannot be in the future", field));
        return;
      }
      int age = date.until(today).getYears();
      if (age < MIN_AGE || age > MAX_AGE) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "age derived from dob must be between " + MIN_AGE + " and " + MAX_AGE,
                field));
      }
    } catch (DateTimeParseException ex) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.VALIDATION_ERROR,
              "dob must be a real calendar date in YYYY-MM-DD",
              field));
    }
  }

  private static void rejectBlank(
      List<ServiceError> errors, String value, String field, String message) {
    if (!StringUtils.hasText(value)) {
      errors.add(ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD, message, field));
    }
  }

  private static void validateSavingsFilter(CreateQuoteCommand command, List<ServiceError> errors) {
    if (command.lob() != Lob.SAVING) {
      return;
    }
    Object raw =
        command.preferences() == null ? null : command.preferences().get("savingsProductType");
    if (raw == null
        || (raw instanceof String s && !StringUtils.hasText(s))
        || (raw instanceof List<?> list && list.isEmpty())) {
      errors.add(
          ServiceError.ofField(
              ErrorCodes.MISSING_REQUIRED_FIELD,
              "savingsProductType is required for Saving quotes",
              "preferences.savingsProductType"));
      return;
    }
    List<?> values = raw instanceof List<?> list ? list : List.of(raw);
    for (Object value : values) {
      if (value == null || LifeContractCatalog.savingsProductTypeWire(value.toString()).isEmpty()) {
        errors.add(
            ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "savingsProductType must be nonParticipating, Participating or ULIP",
                "preferences.savingsProductType"));
        break;
      }
    }
  }

  static boolean isSingleQuote(String mode) {
    if (!StringUtils.hasText(mode)) {
      return false;
    }
    String normalised = mode.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    return "SINGLE".equals(normalised)
        || "SINGLE_QUOTE".equals(normalised)
        || "SQ".equals(normalised);
  }

  private static String firstText(String a, String b) {
    if (StringUtils.hasText(a)) {
      return a.trim();
    }
    return StringUtils.hasText(b) ? b.trim() : null;
  }
}
