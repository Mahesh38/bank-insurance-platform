package com.bank.insurance.onesb.lob.life;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Shared mapping from bank {@link CreateQuoteCommand} to typed {@link LifeQuoteRequest}. LOB
 * handlers supply product family token, optional savings filters, and 1SB paths (DRY).
 */
public final class LifeQuotePayloadFactory {

  public static final String TYPE_MULTI = "Multi-Quote";
  public static final String TYPE_SINGLE = "Single Quote";

  private LifeQuotePayloadFactory() {}

  public static LifeQuoteRequest build(
      CreateQuoteCommand command, SecretProvider secrets, LifeQuoteRequest.Product product) {
    List<LifeQuoteRequest.IndividualDetail> individuals = new ArrayList<>();
    List<CreateQuoteCommand.MemberDetail> members =
        command.members() != null ? command.members() : List.of();
    String quoteCategory = resolveQuoteCategory(command);
    BigDecimal quoteAmount = resolveQuoteAmount(command, quoteCategory);
    int seq = 1;
    for (CreateQuoteCommand.MemberDetail member : members) {
      individuals.add(
          new LifeQuoteRequest.IndividualDetail(
              member.role() != null ? mapMemberType(member.role()) : "Life Assured",
              member.sequenceNumber() > 0 ? member.sequenceNumber() : seq,
              mapGender(member.gender()),
              member.dob() == null ? null : member.dob().trim(),
              member.tobacco() ? "Yes" : "No",
              member.annualIncome(),
              blankToNull(member.pincode()),
              quoteAmount,
              resolveRelationship(command, member)));
      seq++;
    }
    return new LifeQuoteRequest(
        resolveTypeOfQuote(command.mode()),
        quoteCategory,
        "withoutBI",
        "Yes",
        new LifeQuoteRequest.AdditionalSetup("INR", "IN"),
        new LifeQuoteRequest.Distributor(
            secrets.getDistributorId(),
            resolveAgentId(command),
            resolveChannelType(command),
            "Online"),
        new LifeQuoteRequest.PersonalInformation(List.copyOf(individuals)),
        applyTerms(product, command.selection(), command.preferences()));
  }

  public static String resolveTypeOfQuote(String mode) {
    if (!StringUtils.hasText(mode)) {
      return TYPE_MULTI;
    }
    String normalised = mode.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    return switch (normalised) {
      case "SINGLE", "SINGLE_QUOTE", "SQ" -> TYPE_SINGLE;
      default -> TYPE_MULTI;
    };
  }

  public static boolean isSingleQuote(String mode) {
    return TYPE_SINGLE.equals(resolveTypeOfQuote(mode));
  }

  /**
   * SINGLE pin uses {@code selection.*}. MULTI Savings may carry PT/PPT/frequency in preferences
   * (bank SSOT {@code premiumFrequency}, {@code policyTerm}, {@code premiumPayingTerm}) without an
   * insurer pin — apply those onto {@code product.*} so LifeSave gets them (I4).
   */
  private static LifeQuoteRequest.Product applyTerms(
      LifeQuoteRequest.Product product,
      CreateQuoteCommand.ProductSelection selection,
      java.util.Map<String, Object> preferences) {
    if (selection != null && StringUtils.hasText(selection.insurerCode())) {
      List<String> codes =
          selection.productCodes() == null
              ? List.of()
              : selection.productCodes().stream().filter(StringUtils::hasText).toList();
      List<LifeQuoteRequest.InsuranceAndProduct> pin =
          List.of(new LifeQuoteRequest.InsuranceAndProduct(selection.insurerCode().trim(), codes));
      return product.withPin(
          pin,
          option(selection.planOption()),
          option(selection.coverOption()),
          option(selection.deathBenefitOption()),
          selection.policyTerm() != null
              ? selection.policyTerm()
              : intPref(preferences, "policyTerm"),
          selection.premiumPaymentTerm() != null
              ? selection.premiumPaymentTerm()
              : intPref(preferences, "premiumPayingTerm", "premiumPaymentTerm"),
          frequencyWire(
              selection.premiumFrequency() != null
                  ? selection.premiumFrequency()
                  : stringPref(preferences, "premiumFrequency", "paymentFrequency")),
          blankToNull(selection.premiumPaymentOption()));
    }
    Integer policyTerm = intPref(preferences, "policyTerm");
    Integer ppt = intPref(preferences, "premiumPayingTerm", "premiumPaymentTerm");
    String frequency = stringPref(preferences, "premiumFrequency", "paymentFrequency");
    if (policyTerm == null && ppt == null && !StringUtils.hasText(frequency)) {
      return product;
    }
    return product.withPin(
        product.insuranceAndProducts(),
        product.planOption(),
        product.coverOption(),
        product.deathBenefitOption(),
        policyTerm,
        ppt,
        frequencyWire(frequency),
        product.premiumPaymentOption());
  }

  private static String frequencyWire(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    return com.bank.insurance.onesb.application.validation.LifeContractCatalog.frequencyWire(raw)
        .orElse(blankToNull(raw));
  }

  private static Integer intPref(java.util.Map<String, Object> preferences, String... keys) {
    if (preferences == null || preferences.isEmpty()) {
      return null;
    }
    for (String key : keys) {
      Object raw = preferences.get(key);
      if (raw instanceof Number number) {
        return number.intValue();
      }
      if (raw instanceof String text && StringUtils.hasText(text)) {
        try {
          return Integer.valueOf(text.trim());
        } catch (NumberFormatException ignored) {
          // try next key
        }
      }
    }
    return null;
  }

  private static String stringPref(java.util.Map<String, Object> preferences, String... keys) {
    if (preferences == null || preferences.isEmpty()) {
      return null;
    }
    for (String key : keys) {
      Object raw = preferences.get(key);
      if (raw instanceof String text && StringUtils.hasText(text)) {
        return text.trim();
      }
    }
    return null;
  }

  private static LifeQuoteRequest.OptionRef option(String value) {
    return StringUtils.hasText(value) ? new LifeQuoteRequest.OptionRef(value.trim()) : null;
  }

  private static String resolveQuoteCategory(CreateQuoteCommand command) {
    if (command.category() == null || command.category().isBlank()) {
      return "Sum Assured";
    }
    return switch (command.category().trim().toUpperCase().replace(' ', '_')) {
      case "PREMIUM" -> "Premium";
      case "INCOME" -> "Income";
      case "SUM_ASSURED", "SUMASSURED" -> "Sum Assured";
      default -> command.category().trim();
    };
  }

  private static BigDecimal resolveQuoteAmount(CreateQuoteCommand command, String quoteCategory) {
    if ("Premium".equals(quoteCategory) && command.premiumAmount() != null) {
      return command.premiumAmount();
    }
    return command.sumAssured();
  }

  private static String resolveAgentId(CreateQuoteCommand command) {
    if (command.distribution() == null) {
      return "";
    }
    if (command.distribution().agentId() != null && !command.distribution().agentId().isBlank()) {
      return command.distribution().agentId();
    }
    return command.distribution().rmEmployeeId() != null
        ? command.distribution().rmEmployeeId()
        : "";
  }

  private static String resolveChannelType(CreateQuoteCommand command) {
    if (command.distribution() != null
        && command.distribution().channelType() != null
        && !command.distribution().channelType().isBlank()) {
      return command.distribution().channelType();
    }
    return "B2B";
  }

  private static String mapGender(String gender) {
    return com.bank.insurance.onesb.application.validation.LifeContractCatalog.genderWire(gender)
        .orElse(gender == null ? null : gender.trim());
  }

  private static String resolveRelationship(
      CreateQuoteCommand command, CreateQuoteCommand.MemberDetail member) {
    if (member.relationship() != null && !member.relationship().isBlank()) {
      return member.relationship().trim();
    }
    if (isSingleQuote(command.mode())
        && member.role() != null
        && "PROPOSER".equalsIgnoreCase(member.role().trim())) {
      return null;
    }
    return isSingleQuote(command.mode()) ? "Self" : null;
  }

  private static String mapMemberType(String role) {
    if (role == null) {
      return "Life Assured";
    }
    return switch (role.trim().toUpperCase()) {
      case "PROPOSER" -> "Proposer";
      default -> "Life Assured";
    };
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
