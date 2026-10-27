package com.bank.platform.suitability.adapter.mapping;

import com.bank.platform.suitability.domain.BrdCopy;
import com.bank.platform.suitability.domain.JourneyModel.ProductGrid;
import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import com.bank.platform.suitability.domain.JourneyModel.RawAnswers;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Interim insurer grid. Section 22 of the BRD marks the real grid as not yet defined. */
@Component
public class JsonProductGrid implements ProductGrid {

  private final String version;
  private final List<ProductRule> rules;

  public JsonProductGrid(ObjectMapper mapper) {
    try (InputStream in = getClass().getResourceAsStream("/mapping/savings-ulip-interim.json")) {
      if (in == null) {
        this.version = "";
        this.rules = List.of();
        return;
      }
      GridFile file = mapper.readValue(in, GridFile.class);
      this.version = file.version() == null ? "" : file.version();
      this.rules = file.products() == null ? List.of() : List.copyOf(file.products());
    } catch (Exception ex) {
      throw new IllegalStateException("suitability mapping grid could not be read");
    }
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public List<ProductMatch> match(String productType, int age, RawAnswers answers) {
    if (rules.isEmpty() || version.isBlank()) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", "VAL-028", null, BrdCopy.VAL_028);
    }
    List<ProductMatch> matched = new ArrayList<>();
    for (ProductRule rule : rules) {
      if (!rule.productType().equals(productType)) {
        continue;
      }
      if (age < rule.minAge() || age > rule.maxAge()) {
        continue;
      }
      if (!contains(rule.lifeStages(), answers.lifeStage())) {
        continue;
      }
      if (!contains(rule.risks(), answers.risk())) {
        continue;
      }
      if (!contains(rule.goals(), answers.goal())) {
        continue;
      }
      if (!contains(rule.occupations(), answers.occupation())) {
        continue;
      }
      if (!contains(rule.educations(), answers.education())) {
        continue;
      }
      if (!contains(rule.incomeBands(), answers.incomeBand())) {
        continue;
      }
      matched.add(
          new ProductMatch(rule.productId(), rule.insurerCode(), rule.name(), rule.productType()));
    }
    return List.copyOf(matched);
  }

  private static boolean contains(List<String> values, String candidate) {
    return values != null && candidate != null && values.contains(candidate);
  }

  public record GridFile(String version, List<ProductRule> products) {}

  public record ProductRule(
      String productId,
      String productType,
      String insurerCode,
      String name,
      int minAge,
      int maxAge,
      List<String> lifeStages,
      List<String> risks,
      List<String> goals,
      List<String> occupations,
      List<String> educations,
      List<String> incomeBands) {}
}
