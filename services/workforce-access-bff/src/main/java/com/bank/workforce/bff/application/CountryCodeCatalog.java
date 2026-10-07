package com.bank.workforce.bff.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Country calling-code catalogue. R0 ships India only ({@code D-020} companion: mobile search on
 * Lead Screen 1 is +91 / 10 digits). Extra countries are additive.
 */
@Service
public class CountryCodeCatalog {

  private static final List<CountryCode> CODES =
      List.of(new CountryCode("IN", "India", "+91", "Indian", 10, 10, "^[6-9]\\d{9}$"));

  public List<CountryCode> list() {
    return CODES;
  }

  public Optional<CountryCode> find(String countryCode) {
    if (countryCode == null) {
      return Optional.empty();
    }
    return CODES.stream()
        .filter(code -> code.countryCode().equalsIgnoreCase(countryCode))
        .findFirst();
  }

  public MobileValidationResult validate(String countryCode, String nationalNumber) {
    CountryCode spec = find(countryCode).orElse(null);
    if (spec == null) {
      return new MobileValidationResult(false, "UNKNOWN_COUNTRY");
    }
    if (nationalNumber == null || nationalNumber.isBlank()) {
      return new MobileValidationResult(false, "MISSING_NUMBER");
    }
    if (nationalNumber.length() < spec.minLength() || nationalNumber.length() > spec.maxLength()) {
      return new MobileValidationResult(false, "LENGTH");
    }
    if (!Pattern.compile(spec.regex()).matcher(nationalNumber).matches()) {
      return new MobileValidationResult(false, "PATTERN");
    }
    return new MobileValidationResult(true, null);
  }

  public record CountryCode(
      String countryCode,
      String countryName,
      String phoneCode,
      String nationality,
      int minLength,
      int maxLength,
      String regex) {}

  public record MobileValidationResult(boolean valid, String reason) {}
}
