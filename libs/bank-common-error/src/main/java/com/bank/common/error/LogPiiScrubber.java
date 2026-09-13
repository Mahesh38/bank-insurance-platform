package com.bank.common.error;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Framework-level scrubber for strings that will be written to application logs.
 *
 * <p>Defense in depth for {@code S08-G7} / {@code S08-VT-06} / {@code FF-05}: throw sites must not
 * put regulated attributes in {@code reason}, and this scrubber still strips PAN, Aadhaar, phone,
 * email and health-attribute dumps if one does. Applied by {@link Slf4jErrorRecorder} before any
 * log line is emitted.
 */
public final class LogPiiScrubber {

  /** Indian PAN: 5 letters + 4 digits + 1 letter. */
  static final Pattern PAN =
      Pattern.compile("\\b([A-Z]{5})([0-9]{4})([A-Z])\\b", Pattern.CASE_INSENSITIVE);

  /** Aadhaar: 12 digits, optional 4-4-4 grouping. */
  static final Pattern AADHAAR =
      Pattern.compile("(?<!\\d)(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})(?!\\d)");

  /** Indian mobile (optional +91 / 0 prefix). */
  static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?91[\\s-]?)?0?([6-9]\\d{9})(?!\\d)");

  static final Pattern EMAIL =
      Pattern.compile("\\b([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

  /**
   * Health-attribute dumps ({@code diagnosis=…}, {@code medical_condition:…}, ICD-10 style tags).
   * Keywords alone are not scrubbed — only keyed values that would put clinical data in a log
   * index.
   */
  static final Pattern HEALTH =
      Pattern.compile(
          "(?i)\\b(diagnosis|diagnosed|medical[_ -]?condition|pre[_ -]?existing|"
              + "health[_ -]?attribute|icd[- ]?10?)\\s*[:=]\\s*([^\\s,;|]+)");

  private LogPiiScrubber() {}

  /** Returns {@code null} unchanged; otherwise a copy with regulated patterns replaced. */
  public static String scrub(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String result = text;
    result =
        replaceAll(
            result,
            PAN,
            m -> {
              String pan = m.group().toUpperCase(Locale.ROOT);
              return "*".repeat(pan.length() - 5) + pan.substring(pan.length() - 5);
            });
    result = replaceAll(result, AADHAAR, m -> "****-****-" + m.group(3));
    result =
        replaceAll(
            result,
            PHONE,
            m -> {
              String ten = m.group(1);
              return "*".repeat(ten.length() - 4) + ten.substring(ten.length() - 4);
            });
    result =
        replaceAll(
            result,
            EMAIL,
            m -> {
              String local = m.group(1);
              String domain = m.group(2);
              String maskedLocal = local.charAt(0) + "***";
              int dot = domain.lastIndexOf('.');
              String maskedDomain =
                  dot > 0
                      ? domain.charAt(0) + "***" + domain.substring(dot)
                      : domain.charAt(0) + "***";
              return maskedLocal + "@" + maskedDomain;
            });
    result = replaceAll(result, HEALTH, m -> m.group(1) + "=[REDACTED_HEALTH]");
    return result;
  }

  @FunctionalInterface
  private interface Replacer {
    String replace(Matcher m);
  }

  private static String replaceAll(String input, Pattern pattern, Replacer replacer) {
    Matcher matcher = pattern.matcher(input);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacer.replace(matcher)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
