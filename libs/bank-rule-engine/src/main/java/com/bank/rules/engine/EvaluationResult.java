package com.bank.rules.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * The evidence of one evaluation (ADR-024 section 6). The caller persists it whole, together with
 * the rule-set version, so the decision is reproducible from (version, inputs) alone.
 *
 * @param outcome the decided outcome; for {@code COLLECT} without an {@code outcomeRank} it is null
 * @param missingInputs required inputs that were absent or of the wrong type
 * @param evaluatedAt supplied by the caller (the engine never reads a clock), may be null
 */
public record EvaluationResult(
    String ruleSetId,
    String version,
    String engineVersion,
    String inputsHash,
    Map<String, Object> derived,
    String outcome,
    List<Fired> fired,
    List<String> reasonCodes,
    List<Message> messages,
    List<String> missingInputs,
    String evaluatedAt) {

  /**
   * One rule that matched — or that could not be evaluated because a parameter it reads is unset
   * ({@code configMissing} non-empty; DECISIONS D2).
   */
  public record Fired(
      String ruleId,
      String outcome,
      String reasonCode,
      Map<String, Object> params,
      List<String> actions,
      List<String> configMissing) {}

  /** A reason code with its texts, {@code @param} placeholders already substituted. */
  public record Message(String reasonCode, String severity, String rmText, String customerText) {}

  public JsonNode toJson() {
    ObjectNode n = Values.mapper().createObjectNode();
    n.put("ruleSetId", ruleSetId);
    n.put("version", version);
    n.put("engineVersion", engineVersion);
    n.put("inputsHash", inputsHash);
    n.set("derived", Values.toJson(derived));
    n.put("outcome", outcome);
    var firedArr = n.putArray("fired");
    for (Fired f : fired) {
      ObjectNode o = firedArr.addObject();
      o.put("ruleId", f.ruleId());
      o.put("outcome", f.outcome());
      o.put("reasonCode", f.reasonCode());
      o.set("params", Values.toJson(f.params()));
      o.set("actions", Values.toJson(f.actions()));
      o.set("configMissing", Values.toJson(f.configMissing()));
    }
    n.set("reasonCodes", Values.toJson(reasonCodes));
    var msgArr = n.putArray("messages");
    for (Message m : messages) {
      ObjectNode o = msgArr.addObject();
      o.put("reasonCode", m.reasonCode());
      o.put("severity", m.severity());
      o.put("rmText", m.rmText());
      o.put("customerText", m.customerText());
    }
    n.set("missingInputs", Values.toJson(missingInputs));
    n.put("evaluatedAt", evaluatedAt);
    return n;
  }

  /** Canonical bytes: identical inputs and version give an identical string (SUIT-R05). */
  public String toCanonicalJson() {
    try {
      return Values.mapper().writeValueAsString(toJson());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
