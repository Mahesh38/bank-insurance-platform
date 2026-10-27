package com.bank.platform.suitability.application;

import com.bank.platform.suitability.domain.BrdCopy;
import com.bank.platform.suitability.domain.Contacts;
import com.bank.platform.suitability.domain.JourneyModel.RawAnswers;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Merges one screen onto the saved answers. A field that is omitted stays as it was. */
final class AnswerSheet {

  private static final Set<String> FIELDS =
      Set.of(
          "policyFor",
          "lifeAssured",
          "lifeStage",
          "risk",
          "goal",
          "occupation",
          "occupationOther",
          "education",
          "educationOther",
          "incomeBand");
  private static final Set<String> LIFE = Set.of("name", "dob", "gender", "mobile", "email");
  private static final Set<String> SCREENS =
      Set.of("POLICY_FOR", "LIFE_ASSURED", "LIFE_STAGE", "RISK", "GOAL", "PROPOSER_PROFILE");

  private AnswerSheet() {}

  static String merge(String stored, JsonNode patch, ObjectMapper mapper) {
    try {
      ObjectNode base =
          stored == null || stored.isBlank()
              ? mapper.createObjectNode()
              : (ObjectNode) mapper.readTree(stored);
      if (patch != null && patch.isObject()) {
        Iterator<String> names = patch.fieldNames();
        while (names.hasNext()) {
          String name = names.next();
          if ("screen".equals(name)) {
            continue;
          }
          if (!FIELDS.contains(name)) {
            throw new SuitabilityRefusal(
                "VALIDATION_ERROR",
                null,
                name,
                "This question is not part of the suitability assessment.");
          }
          JsonNode value = patch.get(name);
          if (value == null || value.isNull()) {
            continue;
          }
          if ("lifeAssured".equals(name)) {
            mergeLife(base, value);
          } else if (!value.isTextual()) {
            throw new SuitabilityRefusal(
                "VALIDATION_ERROR",
                null,
                name,
                "This question is not part of the suitability assessment.");
          } else {
            base.put(name, value.asText().trim());
          }
        }
      }
      if (BrdCopy.SELF.equals(text(base, "policyFor"))) {
        base.remove("lifeAssured");
      }
      if (base.has("lifeAssured") && base.get("lifeAssured").isObject()) {
        ObjectNode life = (ObjectNode) base.get("lifeAssured");
        life.put("name", BrdCopy.normaliseName(text(life, "name")));
      }
      return mapper.writeValueAsString(base);
    } catch (SuitabilityRefusal refusal) {
      throw refusal;
    } catch (Exception ex) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", "VAL-027", null, BrdCopy.VAL_027);
    }
  }

  static boolean same(String left, String right, ObjectMapper mapper) {
    try {
      JsonNode a = mapper.readTree(left == null || left.isBlank() ? "{}" : left);
      JsonNode b = mapper.readTree(right == null || right.isBlank() ? "{}" : right);
      return a.equals(b);
    } catch (Exception ex) {
      return false;
    }
  }

  static RawAnswers read(String stored, ObjectMapper mapper) {
    try {
      JsonNode node =
          stored == null || stored.isBlank() ? mapper.createObjectNode() : mapper.readTree(stored);
      JsonNode life = node.get("lifeAssured");
      boolean present = life != null && life.isObject();
      return new RawAnswers(
          text(node, "policyFor"),
          present,
          present ? text(life, "name") : "",
          present ? text(life, "dob") : "",
          present ? text(life, "gender") : "",
          present ? text(life, "mobile") : "",
          present ? text(life, "email") : "",
          text(node, "lifeStage"),
          text(node, "risk"),
          text(node, "goal"),
          text(node, "occupation"),
          text(node, "occupationOther"),
          text(node, "education"),
          text(node, "educationOther"),
          text(node, "incomeBand"));
    } catch (Exception ex) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", "VAL-027", null, BrdCopy.VAL_027);
    }
  }

  static String screen(JsonNode patch, RawAnswers answers) {
    if (patch != null && patch.hasNonNull("screen") && patch.get("screen").isTextual()) {
      String requested = patch.get("screen").asText().trim();
      if (SCREENS.contains(requested)) {
        return requested;
      }
    }
    if (!Contacts.blank(answers.incomeBand())
        && !Contacts.blank(answers.occupation())
        && !Contacts.blank(answers.education())) {
      return "PROPOSER_PROFILE";
    }
    if (!Contacts.blank(answers.goal())) {
      return "GOAL";
    }
    if (!Contacts.blank(answers.risk())) {
      return "RISK";
    }
    if (!Contacts.blank(answers.lifeStage())) {
      return "LIFE_STAGE";
    }
    if (!BrdCopy.SELF.equals(answers.policyFor()) && !Contacts.blank(answers.lifeName())) {
      return "LIFE_ASSURED";
    }
    return "POLICY_FOR";
  }

  static Map<String, Object> asMap(String stored, ObjectMapper mapper) {
    try {
      if (stored == null || stored.isBlank()) {
        return Map.of();
      }
      return mapper.readValue(
          stored, mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private static void mergeLife(ObjectNode base, JsonNode value) {
    if (!value.isObject()) {
      throw new SuitabilityRefusal(
          "VALIDATION_ERROR",
          null,
          "lifeAssured",
          "This question is not part of the suitability assessment.");
    }
    ObjectNode life =
        base.has("lifeAssured") && base.get("lifeAssured").isObject()
            ? (ObjectNode) base.get("lifeAssured")
            : base.putObject("lifeAssured");
    Iterator<String> names = value.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      if (!LIFE.contains(name)) {
        throw new SuitabilityRefusal(
            "VALIDATION_ERROR",
            null,
            name,
            "This question is not part of the suitability assessment.");
      }
      JsonNode field = value.get(name);
      if (field == null || field.isNull()) {
        continue;
      }
      if (!field.isTextual()) {
        throw new SuitabilityRefusal(
            "VALIDATION_ERROR",
            null,
            name,
            "This question is not part of the suitability assessment.");
      }
      life.put(name, field.asText().trim());
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return "";
    }
    return value.asText("").trim();
  }
}
