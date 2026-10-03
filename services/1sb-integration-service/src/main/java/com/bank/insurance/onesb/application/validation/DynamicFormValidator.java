package com.bank.insurance.onesb.application.validation;

import com.bank.common.domain.ProposalSchema;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Form usability + answer checks for Gate Criteria and proposal schemas (FUNC-028).
 */
public final class DynamicFormValidator {

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final int MAX_ERRORS = 20;

    private DynamicFormValidator() {}

    public static List<ServiceError> usabilityErrors(ProposalSchema schema) {
        List<ServiceError> errors = new ArrayList<>();
        if (schema == null || schema.fields() == null || schema.fields().isEmpty()) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.SCHEMA_INVALID, "form schema is empty or missing", "schema"));
            return errors;
        }
        List<FieldNode> fields = collectFields(schema.fields());
        Set<String> ids = new LinkedHashSet<>();
        for (FieldNode field : fields) {
            if (StringUtils.hasText(field.id())) {
                ids.add(field.id());
            }
        }
        for (FieldNode field : fields) {
            if (field.mandatory() && !StringUtils.hasText(field.id())) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.SCHEMA_INVALID,
                        "mandatory form control is missing an identifier",
                        "schema"));
            }
            if (field.enumControl() && (field.options() == null || field.options().isEmpty())) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.SCHEMA_INVALID,
                        "enum control '" + nullToUnknown(field.id()) + "' has no option codes",
                        field.id() == null ? "schema" : "schema." + field.id()));
            }
            if (StringUtils.hasText(field.parent()) && !ids.contains(field.parent())) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.SCHEMA_INVALID,
                        "dependency parent '" + field.parent() + "' is not in the form",
                        field.id() == null ? "schema" : "schema." + field.id()));
            }
            if (errors.size() >= MAX_ERRORS) {
                break;
            }
        }
        return List.copyOf(errors);
    }

    public static List<ServiceError> answerErrors(ProposalSchema schema, Map<String, Object> values) {
        List<ServiceError> errors = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "values must include answers for visible mandatory questions",
                    "values"));
            return errors;
        }
        List<FieldNode> fields = schema == null ? List.of() : collectFields(schema.fields());
        for (FieldNode field : fields) {
            boolean visible = isVisible(field, values);
            Object raw = lookup(values, field.id());
            boolean present = present(raw);
            if (field.mandatory() && visible && !present) {
                errors.add(ServiceError.ofField(
                        ErrorCodes.MISSING_REQUIRED_FIELD,
                        "mandatory field missing: " + field.id(),
                        "values." + field.id()));
            }
            if (!present) {
                continue;
            }
            if (field.enumControl() && field.options() != null && !field.options().isEmpty()) {
                String code = String.valueOf(raw).trim();
                if (field.options().stream().noneMatch(o -> o.equals(code))) {
                    boolean label = field.labels().stream().anyMatch(l -> l.equalsIgnoreCase(code));
                    errors.add(ServiceError.ofField(
                            ErrorCodes.VALIDATION_ERROR,
                            label
                                    ? "use the option code, not the display label, for " + field.id()
                                    : "unsupported option code for " + field.id(),
                            "values." + field.id()));
                }
            }
            ServiceError typeError = typeMismatch(field, raw);
            if (typeError != null) {
                errors.add(typeError);
            }
            if (errors.size() >= MAX_ERRORS) {
                break;
            }
        }
        return List.copyOf(errors);
    }

    static List<FieldNode> collectFields(Object node) {
        List<FieldNode> out = new ArrayList<>();
        walk(node, out);
        return out;
    }

    private static void walk(Object node, List<FieldNode> out) {
        if (node instanceof Map<?, ?> map) {
            String id = firstText(map, "name", "id", "fieldName", "fieldId", "key");
            boolean looksLikeField = id != null
                    && (map.containsKey("mandatory") || map.containsKey("required")
                    || map.containsKey("type") || map.containsKey("dataType")
                    || map.containsKey("options") || map.containsKey("enum"));
            if (looksLikeField) {
                out.add(toField(map, id));
            }
            for (Object value : map.values()) {
                walk(value, out);
            }
            return;
        }
        if (node instanceof Collection<?> collection) {
            for (Object item : collection) {
                walk(item, out);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static FieldNode toField(Map<?, ?> map, String id) {
        boolean mandatory = truthy(map.get("mandatory")) || truthy(map.get("required"));
        String type = firstText(map, "type", "dataType", "controlType", "inputType");
        String parent = firstText(map, "parent", "dependsOn", "parentId");
        List<String> options = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        collectOptions(map.get("options"), options, labels);
        collectOptions(map.get("enum"), options, labels);
        collectOptions(map.get("allowedValues"), options, labels);
        boolean enumControl = (type != null && (containsIgnore(type, "enum")
                || containsIgnore(type, "select")
                || containsIgnore(type, "dropdown")
                || containsIgnore(type, "radio")))
                || !options.isEmpty();
        return new FieldNode(id, mandatory, type, parent, List.copyOf(options), List.copyOf(labels), enumControl);
    }

    private static void collectOptions(Object node, List<String> codes, List<String> labels) {
        if (node instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Map<?, ?> map) {
                    String code = firstText(map, "code", "value", "id", "optionCode");
                    String label = firstText(map, "label", "name", "description", "display");
                    if (code != null) {
                        codes.add(code);
                    }
                    if (label != null) {
                        labels.add(label);
                    }
                } else if (item != null) {
                    codes.add(item.toString());
                }
            }
        }
    }

    private static boolean isVisible(FieldNode field, Map<String, Object> values) {
        if (!StringUtils.hasText(field.parent())) {
            return true;
        }
        return present(lookup(values, field.parent()));
    }

    private static Object lookup(Map<String, Object> values, String name) {
        if (values == null || name == null) {
            return null;
        }
        if (values.containsKey(name)) {
            return values.get(name);
        }
        Object nested = values;
        for (String part : name.split("\\.")) {
            if (!(nested instanceof Map<?, ?> map)) {
                return null;
            }
            nested = map.get(part);
        }
        return nested;
    }

    private static boolean present(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return StringUtils.hasText(s);
        }
        if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return !m.isEmpty();
        }
        return true;
    }

    private static ServiceError typeMismatch(FieldNode field, Object raw) {
        if (!StringUtils.hasText(field.type())) {
            return null;
        }
        String type = field.type().toLowerCase(Locale.ROOT);
        try {
            if (type.contains("int") || type.contains("number") || type.contains("amount")) {
                if (raw instanceof Boolean) {
                    return typeError(field, "numeric");
                }
                if (raw instanceof String s && s.isBlank()) {
                    return typeError(field, "numeric");
                }
                if (type.contains("int") && raw instanceof Number n) {
                    if (n.doubleValue() != Math.rint(n.doubleValue())) {
                        return typeError(field, "integer");
                    }
                }
                if (raw instanceof String s) {
                    new BigDecimal(s.trim());
                }
            }
            if (type.contains("date") && raw instanceof String s) {
                LocalDate.parse(s.trim(), ISO_DATE);
            }
            if ((type.contains("bool") || type.contains("yesno")) && raw instanceof String s) {
                String k = s.trim().toLowerCase(Locale.ROOT);
                if (!Set.of("true", "false", "yes", "no", "y", "n", "1", "0").contains(k)) {
                    return typeError(field, "boolean");
                }
            }
        } catch (Exception ex) {
            return typeError(field, field.type());
        }
        return null;
    }

    private static ServiceError typeError(FieldNode field, String expected) {
        return ServiceError.ofField(
                ErrorCodes.VALIDATION_ERROR,
                "value for " + field.id() + " must be " + expected,
                "values." + field.id());
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        String s = value.toString().trim();
        return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s)
                || "1".equals(s);
    }

    private static String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && StringUtils.hasText(v.toString())) {
                return v.toString().trim();
            }
        }
        return null;
    }

    private static boolean containsIgnore(String value, String fragment) {
        return value.toLowerCase(Locale.ROOT).contains(fragment);
    }

    private static String nullToUnknown(String id) {
        return id == null ? "(unnamed)" : id;
    }

    record FieldNode(
            String id,
            boolean mandatory,
            String type,
            String parent,
            List<String> options,
            List<String> labels,
            boolean enumControl) {}
}
