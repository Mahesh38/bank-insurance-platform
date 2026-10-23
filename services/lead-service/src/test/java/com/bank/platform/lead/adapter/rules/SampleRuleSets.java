package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.LeadExceptionFactsPort.Facts;
import com.bank.rules.client.RuleSetQuery;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Test fixtures: the shipped seeds, and LEAD.EXCEPTION overlaid with SAMPLE thresholds (invented
 * for tests, not the BRD owner's values — D2) so configured behaviour can be exercised.
 */
public final class SampleRuleSets {

  private SampleRuleSets() {}

  public static RuleSet seed(String ruleSetId) {
    return RuleSetSeeds.all().stream()
        .map(s -> RuleSetParser.parse(s.json()))
        .filter(r -> r.ruleSetId().equals(ruleSetId))
        .findFirst()
        .orElseThrow();
  }

  /**
   * LEAD.EXCEPTION with SAMPLE parameters: age > 60 → APPROVAL_REQUIRED, blocked customer → BLOCK.
   */
  public static RuleSet configuredLeadException() {
    ObjectNode doc = (ObjectNode) seed("LEAD.EXCEPTION").document().deepCopy();
    JsonNode overlay;
    try (InputStream in =
        SampleRuleSets.class
            .getClassLoader()
            .getResourceAsStream("rulesets/LEAD.EXCEPTION.SAMPLE-configured.json")) {
      overlay = Values.mapper().readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    doc.put("version", overlay.get("version").asText());
    doc.set("parameters", overlay.get("parameters"));
    // The seed's own test cases describe the unset seed; parsing does not run them.
    return RuleSetParser.parse(doc);
  }

  /** A resolver serving exactly these rule sets by domain (no network, no effective dates). */
  public static RuleSetResolver resolverOf(RuleSet... ruleSets) {
    List<RuleSet> all = List.of(ruleSets);
    return new RuleSetResolver() {
      @Override
      public RuleSet resolve(RuleSetQuery query) {
        return all.stream()
            .filter(r -> r.domain().equals(query.domain()))
            .findFirst()
            .orElseThrow(() -> new RuleSetUnresolvableException("none for " + query.domain()));
      }

      @Override
      public RuleSet version(String ruleSetId, String version) {
        throw new RuleSetUnresolvableException("not used");
      }
    };
  }

  /** Every fact known, no breach under the SAMPLE thresholds. */
  public static Facts cleanFacts(int age) {
    return new Facts(
        age,
        false,
        760,
        null,
        null,
        null,
        1,
        LocalDate.parse("2020-01-01"),
        null,
        true,
        false,
        null,
        null,
        null,
        false,
        false,
        Set.of());
  }

  public static Facts blockedCustomer() {
    Facts f = cleanFacts(40);
    return new Facts(
        f.proposerAge(),
        f.hasBsbdAccount(),
        f.equifaxScore(),
        null,
        null,
        null,
        f.activePolicyCount(),
        f.casaOpenedOn(),
        null,
        f.hasActiveRelationship(),
        f.isAuEmployee(),
        null,
        null,
        null,
        false,
        true,
        Set.of());
  }

  public static BigDecimal premium(long value) {
    return BigDecimal.valueOf(value);
  }
}
