package com.bank.insurance.onesb.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks insurance PII (name, mobile, email, PAN/ID, DOB) for logs and audit hashes.
 * Never returns plaintext of recognised PII fields or contiguous PAN/mobile patterns.
 */
public final class PiiMasker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> NAME_FIELDS = Set.of(
            "name", "firstname", "lastname", "fullname", "customername",
            "first_name", "last_name", "full_name", "customer_name");

    private static final Set<String> MOBILE_FIELDS = Set.of(
            "mobile", "mobilenumber", "phone", "phonenumber", "mobile_number", "phone_number");

    private static final Set<String> EMAIL_FIELDS = Set.of(
            "email", "emailaddress", "email_address", "mail");

    private static final Set<String> PAN_FIELDS = Set.of(
            "pan", "pannumber", "customerpan", "pan_number", "customer_pan",
            "panid", "idnumber", "id_number");

    private static final Set<String> DOB_FIELDS = Set.of(
            "dob", "dateofbirth", "birthdate", "date_of_birth", "birth_date");

    /** Indian PAN: 5 letters + 4 digits + 1 letter. */
    private static final Pattern PAN_PATTERN =
            Pattern.compile("\\b([A-Z]{5})([0-9]{4})([A-Z])\\b", Pattern.CASE_INSENSITIVE);

    /** 10-digit Indian mobile (optional +91 / 0 prefix). */
    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("(?<!\\d)(?:\\+?91[\\s-]?)?0?([6-9]\\d{9})(?!\\d)");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    private static final Pattern ISO_DOB_PATTERN =
            Pattern.compile("\\b(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])\\b");

    private PiiMasker() {}

    public static String maskName(String name) {
        if (isBlank(name)) {
            return name;
        }
        String trimmed = name.trim();
        if (trimmed.length() == 1) {
            return "*";
        }
        return trimmed.charAt(0) + "***";
    }

    public static String maskMobile(String mobile) {
        if (isBlank(mobile)) {
            return mobile;
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        String last4 = digits.substring(digits.length() - 4);
        int maskLen = Math.max(digits.length() - 4, 1);
        return "*".repeat(maskLen) + last4;
    }

    public static String maskEmail(String email) {
        if (isBlank(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String maskedLocal = local.charAt(0) + "***";
        int dot = domain.lastIndexOf('.');
        String maskedDomain;
        if (dot > 0) {
            maskedDomain = domain.charAt(0) + "***" + domain.substring(dot);
        } else {
            maskedDomain = domain.charAt(0) + "***";
        }
        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * Masks Indian PAN keeping the last 5 characters (4 digits + check letter),
     * e.g. {@code ABCDE1234F} → {@code *****1234F}. Uses {@code *} (not letters)
     * so the result cannot match a contiguous PAN pattern.
     */
    public static String maskPan(String pan) {
        if (isBlank(pan)) {
            return pan;
        }
        String trimmed = pan.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() <= 5) {
            return "*".repeat(trimmed.length());
        }
        int keep = 5;
        return "*".repeat(trimmed.length() - keep) + trimmed.substring(trimmed.length() - keep);
    }

    public static String maskDob(String dob) {
        if (isBlank(dob)) {
            return dob;
        }
        return "****-**-**";
    }

    /**
     * Masks a value according to a known PII field name (case-insensitive, separators ignored).
     */
    public static String maskField(String fieldName, String value) {
        if (isBlank(value)) {
            return value;
        }
        if (isBlank(fieldName)) {
            return maskText(value);
        }
        String key = normaliseField(fieldName);
        if (NAME_FIELDS.contains(key)) {
            return maskName(value);
        }
        if (MOBILE_FIELDS.contains(key)) {
            return maskMobile(value);
        }
        if (EMAIL_FIELDS.contains(key)) {
            return maskEmail(value);
        }
        if (PAN_FIELDS.contains(key)) {
            return maskPan(value);
        }
        if (DOB_FIELDS.contains(key)) {
            return maskDob(value);
        }
        return maskText(value);
    }

    /**
     * Pattern-based masking for free-form text / JSON strings (PAN, mobile, email, ISO DOB).
     */
    public static String maskText(String text) {
        if (isBlank(text)) {
            return text;
        }
        String result = text;
        result = replaceAll(result, PAN_PATTERN, m -> maskPan(m.group()));
        result = replaceAll(result, MOBILE_PATTERN, m -> {
            String full = m.group();
            String ten = m.group(1);
            String maskedTen = maskMobile(ten);
            if (full.endsWith(ten)) {
                return full.substring(0, full.length() - ten.length()) + maskedTen;
            }
            return maskedTen;
        });
        result = replaceAll(result, EMAIL_PATTERN, m -> maskEmail(m.group()));
        result = replaceAll(result, ISO_DOB_PATTERN, m -> maskDob(m.group()));
        return result;
    }

    /**
     * Masks PII field values in a JSON document; also scrub pattern matches in string leaves.
     */
    public static String maskJson(String json) {
        if (isBlank(json)) {
            return json;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode masked = maskNode(null, root);
            return MAPPER.writeValueAsString(masked);
        } catch (JsonProcessingException e) {
            return maskText(json);
        }
    }

    /**
     * Returns a new map with PII values masked (nested maps processed recursively).
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> maskMap(Map<String, ?> input) {
        if (input == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : input.entrySet()) {
            Object value = e.getValue();
            if (value instanceof Map<?, ?> nested) {
                out.put(e.getKey(), maskMap((Map<String, ?>) nested));
            } else if (value instanceof String s) {
                out.put(e.getKey(), maskField(e.getKey(), s));
            } else {
                out.put(e.getKey(), value);
            }
        }
        return out;
    }

    private static JsonNode maskNode(String fieldName, JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode copy = MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry ->
                    copy.set(entry.getKey(), maskNode(entry.getKey(), entry.getValue())));
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = MAPPER.createArrayNode();
            for (JsonNode child : node) {
                copy.add(maskNode(fieldName, child));
            }
            return copy;
        }
        if (node.isTextual()) {
            String raw = node.asText();
            if (fieldName != null && isKnownPiiField(fieldName)) {
                return TextNode.valueOf(maskField(fieldName, raw));
            }
            return TextNode.valueOf(maskText(raw));
        }
        return node;
    }

    private static boolean isKnownPiiField(String fieldName) {
        String key = normaliseField(fieldName);
        return NAME_FIELDS.contains(key)
                || MOBILE_FIELDS.contains(key)
                || EMAIL_FIELDS.contains(key)
                || PAN_FIELDS.contains(key)
                || DOB_FIELDS.contains(key);
    }

    private static String normaliseField(String fieldName) {
        return fieldName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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
