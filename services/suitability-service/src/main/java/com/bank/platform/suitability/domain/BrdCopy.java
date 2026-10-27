package com.bank.platform.suitability.domain;

/**
 * Seller-facing wording from the Suitability BRD. Current-design section 9.10 wins over section 12.
 */
public final class BrdCopy {

  public static final String SELF = "Self";
  public static final String VAL_001 = "Please select who the policy is for.";
  public static final String VAL_002 = "Please enter the Life Assured Full Name.";
  public static final String VAL_003 = "Full Name can contain letters and spaces only.";
  public static final String VAL_004 = "Please enter the Life Assured Date of Birth.";
  public static final String VAL_005 = "Please enter a valid past Date of Birth.";
  public static final String VAL_006 = "Please enter a valid 10-digit mobile number.";
  public static final String VAL_007 = "Please enter a valid email address.";
  public static final String VAL_008 = "Please select the Life Assured gender.";
  public static final String VAL_009 = "Please select the Proposer life stage.";
  public static final String VAL_010 = "Please select the Proposer risk preference.";
  public static final String VAL_011 = "Please select the Proposer primary goal.";
  public static final String VAL_012 = "Please select the Proposer occupation.";
  public static final String VAL_013 = "Please enter the Proposer occupation.";
  public static final String VAL_014 = "Please select the Proposer education qualification.";
  public static final String VAL_015 = "Please enter the Proposer education qualification.";
  public static final String VAL_016 = "Please enter a valid annual income.";
  public static final String VAL_026 =
      "Proposer Date of Birth is unavailable or invalid. Please update the customer details before continuing.";
  public static final String VAL_027 =
      "We could not save your suitability assessment. Your entered information is retained. Please try again.";
  public static final String VAL_028 =
      "We could not retrieve suitable products at this time. Please try again.";
  public static final String VAL_029 =
      "No suitable product was found based on the customer suitability responses. Please review the responses and try again.";
  public static final String VAL_030 =
      "Suitability cannot be edited after the final Benefit Illustration has been generated.";
  public static final String VAL_031 =
      "This suitability assessment is currently being edited by another user. Please try again later.";
  public static final String VAL_032 =
      "Your suitability responses have been updated. The previously selected product has been cleared. Please complete the assessment again.";

  public static final String PRODUCT_TYPE =
      "This question set applies to Savings Plan and ULIP only.";
  public static final String PENDING_TITLE = "Customer consent pending";
  public static final String PENDING_MESSAGE =
      "A secure link has been sent to the customer\u2019s registered mobile and email to provide consent for sharing of personal details.";
  public static final String RESEND = "Resend link";
  public static final String RECEIVED_TITLE = "Customer consent received";
  public static final String RECEIVED_MESSAGE = "Redirecting to matching plans...";
  public static final String EXIT_TITLE = "Exit journey?";
  public static final String EXIT_MESSAGE =
      "Customer lead progress so far will be saved. You can resume this lead journey anytime from the \u2018Leads\u2019 page.";
  public static final String WAIT = "Please wait a moment and try again.";

  public static final String GOAL_HEALTH = "Health & medical costs";
  public static final String BAND_BELOW_5 = "Below \u20B95L";
  public static final String BAND_5_10 = "\u20B95L \u2013 \u20B910L";
  public static final String BAND_10_20 = "\u20B910L \u2013 \u20B920L";
  public static final String BAND_20 = "\u20B920L and above";

  private BrdCopy() {}

  public static String screenTitle(String screen, String proposerName) {
    String first = firstName(proposerName);
    return switch (screen == null ? "" : screen) {
      case "LIFE_ASSURED" -> "Details of the life assured";
      case "LIFE_STAGE" -> "What life stage is " + first + " in right now?";
      case "RISK" -> "When it comes to growing money, what matters more to " + first + "?";
      case "GOAL" -> "What is " + first + "'s primary goal?";
      case "PROPOSER_PROFILE" -> "A few questions about " + first;
      case "CUSTOMER_CONSENT" -> PENDING_TITLE;
      case "SUITABLE_PRODUCTS" -> "Suitable products";
      default -> "Who are we planning this policy for?";
    };
  }

  public static String firstName(String proposerName) {
    String normalised = normaliseName(proposerName);
    if (normalised.isBlank()) {
      return "the customer";
    }
    int space = normalised.indexOf(' ');
    return space < 0 ? normalised : normalised.substring(0, space);
  }

  public static String normaliseName(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.trim().replaceAll("\\s+", " ");
  }

  public static boolean personName(String normalised) {
    return normalised != null && normalised.matches("^[\\p{L}]+(?: \\p{L}+)*$");
  }
}
