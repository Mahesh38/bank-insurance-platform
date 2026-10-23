package com.bank.rules.engine;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runtime value model and canonical JSON. Numbers are {@link BigDecimal}, dates {@link LocalDate},
 * lists {@link List}, objects {@link Map}. Canonical JSON (sorted keys, plain decimals with
 * trailing zeros stripped) is what {@code inputsHash} and {@code documentSha256} are computed over,
 * so two semantically identical documents hash the same (SUIT-R05).
 */
public final class Values {

  static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
          .build();

  private Values() {}

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  /** Converts a JSON tree into the runtime value model (dates stay strings until typed). */
  public static Object fromJson(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    if (node.isNumber()) {
      return node.decimalValue();
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isTextual()) {
      return node.textValue();
    }
    if (node.isArray()) {
      List<Object> out = new ArrayList<>();
      node.forEach(n -> out.add(fromJson(n)));
      return out;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    Iterator<Map.Entry<String, JsonNode>> it = node.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> e = it.next();
      out.put(e.getKey(), fromJson(e.getValue()));
    }
    return out;
  }

  /** Converts a runtime value to a canonical JSON tree. */
  public static JsonNode toJson(Object value) {
    JsonNodeFactory f = JsonNodeFactory.instance;
    if (value == null) {
      return f.nullNode();
    }
    if (value instanceof BigDecimal d) {
      return f.numberNode(normalize(d));
    }
    if (value instanceof Number n) {
      return f.numberNode(normalize(new BigDecimal(n.toString())));
    }
    if (value instanceof Boolean b) {
      return f.booleanNode(b);
    }
    if (value instanceof List<?> list) {
      var arr = f.arrayNode();
      list.forEach(v -> arr.add(toJson(v)));
      return arr;
    }
    if (value instanceof Map<?, ?> map) {
      ObjectNode obj = f.objectNode();
      new TreeMap<>(map).forEach((k, v) -> obj.set(String.valueOf(k), toJson(v)));
      return obj;
    }
    if (value instanceof JsonNode node) {
      return canonical(node);
    }
    return f.textNode(value.toString());
  }

  /** A canonical copy of {@code node}: sorted object keys, normalised numbers. */
  public static JsonNode canonical(JsonNode node) {
    return toJson(fromJson(node));
  }

  public static String canonicalString(JsonNode node) {
    try {
      return MAPPER.writeValueAsString(canonical(node));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("canonical JSON failed", e);
    }
  }

  public static String sha256(String text) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  static BigDecimal normalize(BigDecimal d) {
    BigDecimal s = d.stripTrailingZeros();
    return s.scale() < 0 ? s.setScale(0) : s;
  }

  /** A DATE value: {@code yyyy-MM-dd}, or an RFC 3339 date-time taken at its own offset's date. */
  static LocalDate toDate(Object value) {
    if (value instanceof LocalDate d) {
      return d;
    }
    if (value instanceof String s) {
      try {
        return s.length() <= 10 ? LocalDate.parse(s) : OffsetDateTime.parse(s).toLocalDate();
      } catch (DateTimeParseException e) {
        return null;
      }
    }
    return null;
  }

  /** Text form used in rendered messages. */
  static String render(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof BigDecimal d) {
      return normalize(d).toPlainString();
    }
    if (value instanceof List<?> list) {
      List<String> parts = new ArrayList<>();
      list.forEach(v -> parts.add(render(v)));
      return String.join(", ", parts);
    }
    return value.toString();
  }
}
